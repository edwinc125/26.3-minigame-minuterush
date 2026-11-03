package com.edwinc125.minuterush.game;

import com.edwinc125.minuterush.config.MinuteRushConfig;
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
		resetPreparation();
		participants.addAll(players);
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
			player.teleportTo(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
			player.setRespawnPosition(new RespawnConfig(
				LevelData.RespawnData.of(level.dimension(), position, 0.0f, 0.0f), true), true);
			player.setGameMode(GameType.SURVIVAL);
		}
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

	public void addSpectator(ServerPlayer player) {
		originalGameModes.putIfAbsent(player.getUUID(), player.gameMode());
		spectators.add(player.getUUID());
		player.setGameMode(GameType.SPECTATOR);
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
		clearTaskBlocks();
		release();
		cleanupActive = true;
		cleanupTeleported = false;
		cleanupDelayTicks = 1;
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
