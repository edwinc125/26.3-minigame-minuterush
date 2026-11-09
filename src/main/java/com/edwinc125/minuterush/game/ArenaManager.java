package com.edwinc125.minuterush.game;

import com.edwinc125.minuterush.config.MinuteRushConfig;
import com.edwinc125.minuterush.task.TaskRequirements;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Util;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.levelgen.RandomState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.IdentityHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.server.level.ServerPlayer.RespawnConfig;

public final class ArenaManager {
	private final MinecraftServer server;
	private final Map<UUID, BlockPos> frozenPositions = new HashMap<>();
	private final Map<UUID, GameType> originalGameModes = new HashMap<>();
	private final Set<UUID> spectators = new HashSet<>();
	private final List<ServerPlayer> participants = new ArrayList<>();
	private final List<BlockPos> preparedPositions = new ArrayList<>();
	private final AtomicInteger loadedChunks = new AtomicInteger();
	private final AtomicInteger failedChunks = new AtomicInteger();
	private final AtomicInteger preparationGeneration = new AtomicInteger();
	private static final Map<ServerLevel, Set<BlockPos>> TASK_BLOCKS = new IdentityHashMap<>();
	private static final int FREEZE_AMPLIFIER = 200;
	private static final int ARENA_CHUNK_WIDTH = 10;
	private static final int ARENA_CHUNK_DEPTH = 10;
	private WorldBorderSnapshot originalBorder;
	private boolean originalKeepInventory;
	private boolean keepInventoryCaptured;
	private boolean preparing;
	private boolean ready;
	private int totalChunks;
	private int generatedChunks;
	private int centerX;
	private int centerZ;
	private long keepInventoryUntil;
	private boolean cleanupActive;
	private int cleanupDelayTicks;
	private boolean cleanupTeleported;
	private final List<ServerPlayer> cleanupPlayers = new ArrayList<>();
	/**
	 * Match players who were offline when the match ended, with the game mode to restore. Cleanup only reaches
	 * online players, so these are sent back to the lobby when they reconnect. Survives resetPreparation.
	 */
	private final Map<UUID, GameType> pendingLobbyReturns = new HashMap<>();
	/** Biome sample spacing in blocks; 16 gives ~4000 samples for a 1000-block border. */
	private static final int SURVEY_STEP = 16;
	private volatile ArenaSurvey<Holder<Biome>> survey;
	private int surveyRadius = 500;

	public ArenaManager(MinecraftServer server) {
		this.server = server;
	}

	public boolean preparing() {
		return preparing;
	}

	public boolean ready() {
		return ready;
	}

	public int generatedChunks() {
		return loadedChunks.get();
	}

	public int totalChunks() {
		return totalChunks;
	}

	public void beginPreparation(List<ServerPlayer> players, MinuteRushConfig config) {
		beginPreparation(players, List.of(), config);
	}

	public void beginPreparation(List<ServerPlayer> players, List<ServerPlayer> spectatorPlayers,
			MinuteRushConfig config) {
		resetPreparation();
		surveyRadius = config.maxBorderSize() / 2;
		participants.addAll(players);
		for (ServerPlayer spectator : spectatorPlayers) {
			originalGameModes.putIfAbsent(spectator.getUUID(), spectator.gameMode());
			spectators.add(spectator.getUUID());
			spectator.setGameMode(GameType.SPECTATOR);
		}
		ServerLevel level = server.overworld();
		BlockPos spawn = chooseArenaCenter(level);
		centerX = spawn.getX();
		centerZ = spawn.getZ();
		originalBorder = WorldBorderSnapshot.capture(level.getWorldBorder());
		originalKeepInventory = level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY);
		keepInventoryCaptured = true;
		totalChunks = ARENA_CHUNK_WIDTH * ARENA_CHUNK_DEPTH;
		loadedChunks.set(0);
		failedChunks.set(0);
		preparing = true;
		ready = false;
		freeze(players);
		startChunkLoading(level);
	}

	public static void registerTaskBlock(ServerLevel level, BlockPos position) {
		TASK_BLOCKS.computeIfAbsent(level, ignored -> new HashSet<>()).add(position.immutable());
	}

	public void clearTaskBlocks() {
		ServerLevel level = server.overworld();
		Set<BlockPos> positions = TASK_BLOCKS.remove(level);
		if (positions == null) {
			return;
		}
		for (BlockPos position : positions) {
			level.setBlock(position, Blocks.AIR.defaultBlockState(), 3);
		}
	}

	public void tickPreparation() {
		if (!preparing) {
			return;
		}
		ServerLevel level = server.overworld();
		generatedChunks = loadedChunks.get();
		if (failedChunks.get() > 0) {
			restartPreparation(level);
			return;
		}
		if (loadedChunks.get() == totalChunks && prepareParticipantPositions(level)) {
			preparing = false;
			ready = true;
		}
	}

	/**
	 * Surveys the largest border the arena can reach, entirely off the server thread: samples surface biomes
	 * every SURVEY_STEP blocks, then predicts villages from their placement rules. Both only evaluate seed-based
	 * worldgen math (the same the worldgen worker threads run), so no chunks are generated or loaded.
	 */
	private void startSurvey(ServerLevel level) {
		ArenaSurvey<Holder<Biome>> next = new ArenaSurvey<>();
		survey = next;
		int cx = centerX;
		int cz = centerZ;
		int radius = surveyRadius;
		BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
		RandomState randomState = level.getChunkSource().randomState();
		ChunkGeneratorStructureState structureState = level.getChunkSource().getGeneratorState();
		// Resolve tags on the server thread; the worker only reads the resulting list.
		List<StructureSet> villageSets = structureState.possibleStructureSets().stream()
			.map(Holder::value)
			.filter(set -> set.structures().stream().anyMatch(entry -> entry.structure().is(StructureTags.VILLAGE)))
			.toList();
		// High above the terrain, so the sampled biome is the surface biome rather than a cave biome.
		int quartY = QuartPos.fromBlock(256);
		CompletableFuture.runAsync(() -> {
			// Created on the worker thread: an uncached resolver is not shared with world generation.
			BiomeResolver resolver = source.createUncachedResolver(randomState);
			for (int dx = -radius; dx <= radius; dx += SURVEY_STEP) {
				for (int dz = -radius; dz <= radius; dz += SURVEY_STEP) {
					Holder<Biome> biome = resolver.getNoiseBiome(
						QuartPos.fromBlock(cx + dx), quartY, QuartPos.fromBlock(cz + dz));
					next.addBiomeSample(biome, dx, dz);
				}
			}
			next.markBiomesScanned();
			findNearestVillage(next, villageSets, structureState, resolver, cx, cz, radius, quartY);
		}, Util.backgroundExecutor()).exceptionally(error -> {
			com.mojang.logging.LogUtils.getLogger().warn("Minute Rush arena survey failed", error);
			return null;
		});
	}

	/**
	 * Predicts the nearest village the way world generation places them: each spacing x spacing chunk region
	 * has one seed-determined candidate chunk, which gets a village if a village type in the set accepts the
	 * biome there. Runs on the survey worker thread. Unlike /locate it does not load chunks; the trade-off is
	 * that it can't see a village that later fails to fit the terrain, which is rare.
	 */
	private static void findNearestVillage(ArenaSurvey<Holder<Biome>> survey, List<StructureSet> villageSets,
			ChunkGeneratorStructureState structureState, BiomeResolver resolver, int cx, int cz, int radius,
			int quartY) {
		long seed = structureState.getLevelSeed();
		int bestDx = 0;
		int bestDz = 0;
		int best = ArenaSurvey.NONE;
		for (StructureSet set : villageSets) {
			if (!(set.placement() instanceof RandomSpreadStructurePlacement spread)) {
				continue;
			}
			int spacing = spread.spacing();
			int minRegionX = Math.floorDiv((cx - radius) >> 4, spacing);
			int maxRegionX = Math.floorDiv((cx + radius) >> 4, spacing);
			int minRegionZ = Math.floorDiv((cz - radius) >> 4, spacing);
			int maxRegionZ = Math.floorDiv((cz + radius) >> 4, spacing);
			for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
				for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
					ChunkPos candidate = spread.getPotentialStructureChunk(seed, regionX * spacing, regionZ * spacing);
					int dx = candidate.getMiddleBlockX() - cx;
					int dz = candidate.getMiddleBlockZ() - cz;
					int distance = ArenaSurvey.distance(dx, dz);
					if (distance > radius || distance >= best
							|| !spread.isStructureChunk(structureState, candidate.x(), candidate.z())) {
						continue;
					}
					Holder<Biome> biome = resolver.getNoiseBiome(QuartPos.fromBlock(candidate.getMiddleBlockX()),
						quartY, QuartPos.fromBlock(candidate.getMiddleBlockZ()));
					if (set.structures().stream().anyMatch(entry -> entry.structure().value().biomes().contains(biome))) {
						best = distance;
						bestDx = dx;
						bestDz = dz;
					}
				}
			}
		}
		if (best == ArenaSurvey.NONE) {
			survey.markNoVillage();
		} else {
			survey.setVillageOffset(bestDx, bestDz);
		}
	}

	/**
	 * Whether a task's biome or village (see {@link TaskRequirements}) lies inside the current world border.
	 * Tasks with requirements are unavailable until the survey has finished.
	 */
	public boolean taskAvailable(String taskId) {
		TaskRequirements.Requirement requirement = TaskRequirements.of(taskId);
		if (requirement == null) {
			return true;
		}
		ArenaSurvey<Holder<Biome>> current = survey;
		if (current == null) {
			return false;
		}
		int halfSize = (int) (server.overworld().getWorldBorder().getSize() / 2);
		return requirement.isMet(biome -> current.hasBiomeWithin(biome, halfSize), current.hasVillageWithin(halfSize));
	}

	public void placeParticipants(MinuteRushConfig config) {
		if (!ready) {
			return;
		}
		ServerLevel level = server.overworld();
		WorldBorder border = level.getWorldBorder();
		border.setCenter(centerX, centerZ);
		border.setSize(Math.min(config.maxBorderSize(),
			Math.max(ARENA_CHUNK_WIDTH, ARENA_CHUNK_DEPTH) * 16.0));
		border.setWarningTime(5);
		border.setWarningBlocks(5);
		for (int i = 0; i < participants.size(); i++) {
			ServerPlayer player = participants.get(i);
			BlockPos position = preparedPositions.size() > i ? preparedPositions.get(i) : null;
			if (position == null) {
				position = new BlockPos(centerX, 80, centerZ);
			}
			frozenPositions.put(player.getUUID(), position);
			resetForMatch(player);
			player.teleportTo(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
			player.setRespawnPosition(new RespawnConfig(
				LevelData.RespawnData.of(level.dimension(), position, 0.0f, 0.0f), true), true);
			player.setGameMode(GameType.SURVIVAL);
		}
		for (UUID spectatorId : spectators) {
			ServerPlayer spectator = server.getPlayerList().getPlayer(spectatorId);
			if (spectator != null) {
				sendSpectatorToArena(spectator);
			}
		}
		// 26.x uses world clocks with time markers, so let the vanilla command move the overworld clock.
		server.getCommands().performPrefixedCommand(
			server.createCommandSourceStack().withSuppressedOutput(), "time set day");
	}

	/** Gives every participant the same start: empty inventory, full health and hunger, no effects. */
	private static void resetForMatch(ServerPlayer player) {
		player.getInventory().clearContent();
		player.inventoryMenu.getCraftSlots().clearContent();
		player.containerMenu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
		player.removeAllEffects();
		player.setHealth(player.getMaxHealth());
		player.getFoodData().setFoodLevel(20);
		player.getFoodData().setSaturation(5.0f);
		player.clearFire();
		player.setAirSupply(player.getMaxAirSupply());
		player.resetFallDistance();
	}

	public void expandBorder(MinuteRushConfig config) {
		if (!ready || preparing) {
			return;
		}
		WorldBorder border = server.overworld().getWorldBorder();
		double target = Math.min(config.maxBorderSize(),
			border.getSize() + config.borderExpansionBlocksPerSecond() / 20.0);
		border.setSize(target);
	}

	public void freeze(List<ServerPlayer> players) {
		for (ServerPlayer player : players) {
			frozenPositions.putIfAbsent(player.getUUID(), player.blockPosition());
			player.setDeltaMovement(0, 0, 0);
			player.setNoGravity(true);
			applyFreezeEffects(player);
		}
	}

	public void holdFrozen() {
		for (ServerPlayer player : participants) {
			BlockPos position = frozenPositions.get(player.getUUID());
			if (position != null) {
				player.teleportTo(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
				player.setDeltaMovement(0, 0, 0);
			}
			applyFreezeEffects(player);
		}
	}

	public void release() {
		for (ServerPlayer player : participants) {
			player.setNoGravity(false);
			clearFreezeEffects(player);
		}
		frozenPositions.clear();
	}

	/**
	 * A participant reconnected. Reconnecting creates a new ServerPlayer object, so swap out the stale
	 * one; otherwise placing, freezing and releasing would act on the disconnected object.
	 */
	public void rejoinParticipant(ServerPlayer player) {
		participants.removeIf(existing -> existing.getUUID().equals(player.getUUID()));
		participants.add(player);
		if (frozenPositions.containsKey(player.getUUID())) {
			// Still in the pre-game freeze; they may have been offline when players were placed.
			resetForMatch(player);
			player.setGameMode(GameType.SURVIVAL);
			player.setNoGravity(true);
			applyFreezeEffects(player);
		} else {
			// The freeze may have ended while they were offline; don't leave them floating.
			player.setNoGravity(false);
			clearFreezeEffects(player);
		}
	}

	public void addSpectator(ServerPlayer player) {
		originalGameModes.putIfAbsent(player.getUUID(), player.gameMode());
		spectators.add(player.getUUID());
		player.setGameMode(GameType.SPECTATOR);
		if (ready) {
			sendSpectatorToArena(player);
		}
		// Otherwise the arena is still generating; placeParticipants moves spectators once it is ready.
	}

	private void sendSpectatorToArena(ServerPlayer player) {
		BlockPos spawn = preparedPositions.isEmpty() ? new BlockPos(centerX, 80, centerZ) : preparedPositions.get(0);
		// Cross-dimension teleport, in case the spectator logged in elsewhere (e.g. the Nether).
		player.teleportTo(server.overworld(), spawn.getX() + 0.5, spawn.getY() + 2, spawn.getZ() + 0.5,
			Set.of(), player.getYRot(), player.getXRot(), true);
	}

	public boolean isSpectator(ServerPlayer player) {
		return spectators.contains(player.getUUID());
	}

	public void enableKeepInventory(int seconds) {
		if (!keepInventoryCaptured) {
			originalKeepInventory = server.overworld().getGameRules()
				.get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY);
			keepInventoryCaptured = true;
		}
		server.overworld().getGameRules().set(
			net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY, true, server);
		keepInventoryUntil = Math.max(keepInventoryUntil, server.overworld().getGameTime() + seconds * 20L);
	}

	public void tickModifiers() {
		if (keepInventoryUntil > 0 && server.overworld().getGameTime() >= keepInventoryUntil) {
			restoreKeepInventory();
			keepInventoryUntil = 0;
		}
	}

	public boolean cleanupActive() {
		return cleanupActive;
	}

	public boolean tickCleanup() {
		if (!cleanupActive) {
			return true;
		}
		if (--cleanupDelayTicks > 0) {
			return false;
		}
		ServerLevel level = server.overworld();
		BlockPos lobby = server.getRespawnData().pos();
		RespawnConfig lobbyRespawn = new RespawnConfig(
			LevelData.RespawnData.of(level.dimension(), lobby, 0.0f, 0.0f), true);
		if (!cleanupTeleported) {
			for (ServerPlayer player : cleanupPlayers) {
				player.teleportTo(lobby.getX() + 0.5, lobby.getY(), lobby.getZ() + 0.5);
			}
			cleanupTeleported = true;
			cleanupDelayTicks = 1;
			return false;
		}
		for (ServerPlayer player : cleanupPlayers) {
			player.setRespawnPosition(lobbyRespawn, true);
			player.getInventory().clearContent();
			player.removeAllEffects();
			player.setNoGravity(false);
			player.setGameMode(spectators.contains(player.getUUID())
				? originalGameModes.getOrDefault(player.getUUID(), GameType.SURVIVAL)
				: GameType.SURVIVAL);
		}
		restoreKeepInventory();
		if (originalBorder != null) {
			originalBorder.apply(level.getWorldBorder());
		}
		cleanupPlayers.clear();
		cleanupActive = false;
		cleanupTeleported = false;
		resetPreparation();
		return true;
	}

	public void restoreLobbyState() {
		if (cleanupActive) {
			return;
		}
		ServerLevel level = server.overworld();
		BlockPos lobby = server.getRespawnData().pos();
		WorldBorder border = level.getWorldBorder();
		if (originalBorder == null) {
			originalBorder = WorldBorderSnapshot.capture(border);
		}
		border.setCenter(lobby.getX(), lobby.getZ());
		border.setSize(30_000_000.0);
		RespawnConfig lobbyRespawn = new RespawnConfig(
			LevelData.RespawnData.of(level.dimension(), lobby, 0.0f, 0.0f), true);
		cleanupPlayers.clear();
		cleanupPlayers.addAll(server.getPlayerList().getPlayers());
		for (ServerPlayer player : cleanupPlayers) {
			player.setRespawnPosition(lobbyRespawn, true);
		}
		for (ServerPlayer participant : participants) {
			if (server.getPlayerList().getPlayer(participant.getUUID()) == null) {
				pendingLobbyReturns.put(participant.getUUID(), GameType.SURVIVAL);
			}
		}
		for (UUID spectatorId : spectators) {
			if (server.getPlayerList().getPlayer(spectatorId) == null) {
				pendingLobbyReturns.put(spectatorId, originalGameModes.getOrDefault(spectatorId, GameType.SURVIVAL));
			}
		}
		clearTaskBlocks();
		release();
		cleanupActive = true;
		cleanupTeleported = false;
		cleanupDelayTicks = 1;
	}

	/**
	 * A player who was offline when the last match ended reconnected: they log back in wherever they left the
	 * arena, so give them the same lobby cleanup everyone else got. Returns whether anything was done.
	 */
	public boolean returnToLobbyIfPending(ServerPlayer player) {
		GameType gameMode = pendingLobbyReturns.remove(player.getUUID());
		if (gameMode == null) {
			return false;
		}
		ServerLevel level = server.overworld();
		BlockPos lobby = server.getRespawnData().pos();
		// Cross-dimension teleport, in case they left the match from the Nether or the End.
		player.teleportTo(level, lobby.getX() + 0.5, lobby.getY(), lobby.getZ() + 0.5,
			Set.of(), 0.0f, 0.0f, true);
		player.setRespawnPosition(new RespawnConfig(
			LevelData.RespawnData.of(level.dimension(), lobby, 0.0f, 0.0f), true), true);
		player.getInventory().clearContent();
		player.removeAllEffects();
		player.setNoGravity(false);
		player.setGameMode(gameMode);
		return true;
	}

	public void resetPreparation() {
		preparing = false;
		ready = false;
		participants.clear();
		preparedPositions.clear();
		preparationGeneration.incrementAndGet();
		frozenPositions.clear();
		spectators.clear();
		originalGameModes.clear();
		generatedChunks = 0;
		totalChunks = 0;
		centerX = 0;
		centerZ = 0;
		keepInventoryUntil = 0;
	}

	private void restoreKeepInventory() {
		if (keepInventoryCaptured) {
			server.overworld().getGameRules().set(
				net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY,
				originalKeepInventory, server);
			keepInventoryCaptured = false;
		}
	}

	private static void applyFreezeEffects(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, FREEZE_AMPLIFIER,
			false, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 40, FREEZE_AMPLIFIER,
			false, false, false));
	}

	private static void clearFreezeEffects(ServerPlayer player) {
		if (player.hasEffect(MobEffects.SLOWNESS)
				&& player.getEffect(MobEffects.SLOWNESS).getAmplifier() >= FREEZE_AMPLIFIER) {
			player.removeEffect(MobEffects.SLOWNESS);
		}
		if (player.hasEffect(MobEffects.JUMP_BOOST)
				&& player.getEffect(MobEffects.JUMP_BOOST).getAmplifier() >= FREEZE_AMPLIFIER) {
			player.removeEffect(MobEffects.JUMP_BOOST);
		}
	}

	private BlockPos safePosition(ServerLevel level, int index, int count) {
		double angle = count == 0 ? 0 : (Math.PI * 2.0 * index / count);
		int baseX = centerX + (int) Math.round(Math.cos(angle) * 8);
		int baseZ = centerZ + (int) Math.round(Math.sin(angle) * 8);
		for (int step = 0; step <= 2; step++) {
			int distance = step * 32;
			for (int xDirection : new int[] { -1, 0, 1 }) {
				for (int zDirection : new int[] { -1, 0, 1 }) {
					if (xDirection == 0 && zDirection == 0 && step != 0) {
						continue;
					}
					BlockPos candidate = surfacePosition(level,
						baseX + xDirection * distance, baseZ + zDirection * distance);
					if (candidate != null) {
						return candidate;
					}
				}
			}
			for (int offset = -step; offset <= step; offset++) {
				BlockPos candidate = surfacePosition(level, baseX + offset * 32, baseZ);
				if (candidate != null) {
					return candidate;
				}
				candidate = surfacePosition(level, baseX, baseZ + offset * 32);
				if (candidate != null) {
					return candidate;
				}
			}
		}
		return null;
	}

	private static BlockPos surfacePosition(ServerLevel level, int x, int z) {
		ChunkAccess chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
		if (chunk == null) {
			return null;
		}
		Heightmap heightmap = chunk.getHeightmaps().stream()
			.filter(entry -> entry.getKey() == Heightmap.Types.WORLD_SURFACE)
			.map(Map.Entry::getValue)
			.findFirst()
			.orElse(null);
		if (heightmap == null) {
			return null;
		}
		int y = heightmap.getFirstAvailable(x & 15, z & 15);
		if (y <= level.dimensionType().minY()) {
			return null;
		}
		BlockPos surface = new BlockPos(x, y - 1, z);
		BlockState state = level.getBlockState(surface);
		if (state.isAir()
				|| state.getFluidState().is(FluidTags.WATER)
				|| state.getFluidState().is(FluidTags.LAVA)
				|| !state.getFluidState().isEmpty()
				|| !state.isSolid()) {
			return null;
		}
		return surface.above();
	}

	private boolean prepareParticipantPositions(ServerLevel level) {
		preparedPositions.clear();
		for (int i = 0; i < participants.size(); i++) {
			BlockPos position = safePosition(level, i, participants.size());
			if (position == null) {
				preparedPositions.clear();
				return false;
			}
			preparedPositions.add(position);
		}
		return true;
	}

	private void restartPreparation(ServerLevel level) {
		preparedPositions.clear();
		BlockPos spawn = chooseArenaCenter(level);
		centerX = spawn.getX();
		centerZ = spawn.getZ();
		loadedChunks.set(0);
		failedChunks.set(0);
		totalChunks = ARENA_CHUNK_WIDTH * ARENA_CHUNK_DEPTH;
		preparationGeneration.incrementAndGet();
		freeze(participants);
		startChunkLoading(level);
	}

	private void startChunkLoading(ServerLevel level) {
		int generation = preparationGeneration.get();
		startSurvey(level);
		int halfWidth = ARENA_CHUNK_WIDTH / 2;
		int halfDepth = ARENA_CHUNK_DEPTH / 2;
		for (int x = -halfWidth; x < ARENA_CHUNK_WIDTH - halfWidth; x++) {
			for (int z = -halfDepth; z < ARENA_CHUNK_DEPTH - halfDepth; z++) {
				int chunkX = (centerX >> 4) + x;
				int chunkZ = (centerZ >> 4) + z;
				CompletableFuture<ChunkResult<ChunkAccess>> future =
					level.getChunkSource().getChunkFuture(chunkX, chunkZ, ChunkStatus.FULL, true);
				future.thenAccept(result -> {
					if (preparationGeneration.get() != generation) {
						return;
					}
					if (result.isSuccess()) {
						loadedChunks.incrementAndGet();
					} else {
						failedChunks.incrementAndGet();
					}
				}).exceptionally(error -> {
					if (preparationGeneration.get() == generation) {
						failedChunks.incrementAndGet();
					}
					return null;
				});
			}
		}
	}

	private static BlockPos chooseArenaCenter(ServerLevel level) {
		BlockPos lobby = level.getServer().getRespawnData().pos();
		Random random = new Random();
		for (int attempt = 0; attempt < 512; attempt++) {
			int distance = 3_000 + random.nextInt(27_001);
			double angle = random.nextDouble() * Math.PI * 2.0;
			int x = lobby.getX() + (int) Math.round(Math.cos(angle) * distance);
			int z = lobby.getZ() + (int) Math.round(Math.sin(angle) * distance);
			BlockPos target = new BlockPos(x, 80, z);
			var biomeHolder = level.getBiome(target);
			var biome = biomeHolder.value();
			String biomeName = biomeHolder.unwrapKey()
				.map(key -> key.identifier().getPath())
				.orElse("");
			if (!biomeName.contains("ocean")
					&& !biomeName.equals("deep_ocean")
					&& !biomeName.equals("frozen_ocean")
					&& !biomeName.equals("river")) {
				return target;
			}
		}
		int distance = 3_000 + random.nextInt(27_001);
		double angle = random.nextDouble() * Math.PI * 2.0;
		return new BlockPos(
			lobby.getX() + (int) Math.round(Math.cos(angle) * distance),
			80,
			lobby.getZ() + (int) Math.round(Math.sin(angle) * distance));
	}

	private static long packChunk(int x, int z) {
		return ((long) x << 32) ^ (z & 0xffffffffL);
	}

	private static int unpackX(long packed) {
		return (int) (packed >> 32);
	}

	private static int unpackZ(long packed) {
		return (int) packed;
	}

	private record WorldBorderSnapshot(double centerX, double centerZ, double size,
			double damagePerBlock, int warningTime, int warningBlocks) {
		private static WorldBorderSnapshot capture(WorldBorder border) {
			return new WorldBorderSnapshot(border.getCenterX(), border.getCenterZ(), border.getSize(),
				border.getDamagePerBlock(), border.getWarningTime(), border.getWarningBlocks());
		}

		private void apply(WorldBorder border) {
			border.setCenter(centerX, centerZ);
			border.setSize(size);
			border.setDamagePerBlock(damagePerBlock);
			border.setWarningTime(warningTime);
			border.setWarningBlocks(warningBlocks);
		}
	}
}
