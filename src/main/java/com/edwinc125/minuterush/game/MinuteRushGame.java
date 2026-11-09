package com.edwinc125.minuterush.game;

import com.edwinc125.minuterush.config.MinuteRushConfig;
import com.edwinc125.minuterush.task.MinuteRushTask;
import com.edwinc125.minuterush.task.TaskFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.core.registries.BuiltInRegistries;

public final class MinuteRushGame {
	private final MinecraftServer server;
	private MinuteRushConfig config;
	private final Map<UUID, Integer> scores = new HashMap<>();
	private final java.util.Set<UUID> participants = new java.util.HashSet<>();
	private final java.util.Set<UUID> lobbyReady = new java.util.HashSet<>();
	private final java.util.Set<UUID> lobbySpectators = new java.util.HashSet<>();
	private final Deque<String> recentTaskIds = new ArrayDeque<>();
	/** Decides how likely each task difficulty is as the match goes on. */
	private final com.edwinc125.minuterush.task.DifficultyProgress difficultyProgress =
		new com.edwinc125.minuterush.task.DifficultyProgress();
	/** Ticks since the first round of the match started; drives progressive difficulty. */
	private long matchTicks;
	private final Map<UUID, String> previousStandingBlocks = new HashMap<>();
	private final Random random = new Random();
	private final SkipVote skipVote = new SkipVote();
	/** Players whose glowing tag was turned on by a player task, so only those are turned off again. */
	private final Set<UUID> glowTagged = new java.util.HashSet<>();
	public static final String VOTE_RECORDED = "Vote recorded.";
	private final ArenaManager arena;
	private MatchState state = MatchState.IDLE;
	private MinuteRushTask task;
	private ServerPlayer testPlayer;
	private String testCategory;
	private List<TaskFactory.Selection> testSelections = List.of();
	private int testIndex;
	private MinuteRushTask testTask;
	private int ticksRemaining;
	private int countdownTicks;
	private int intermissionTicks;
	/** Seconds players stay in the arena after the match is won, before being sent back to the lobby. */
	private static final int MATCH_END_DELAY_SECONDS = 5;
	private int matchOverTicks;
	private int lobbyCountdownTicks;
	private int lastIntermissionSecond = -1;
	private String currentTaskId;
	private String intermissionMessage;
	private boolean cleanupPending;
	private final ServerBossEvent bossbar = new ServerBossEvent(
		UUID.fromString("a0e1e2d3-c4b5-4678-9012-3456789abcde"),
		Component.literal("Minute Rush").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
		BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);

	public MinuteRushGame(MinecraftServer server) {
		this(server, MinuteRushConfig.DEFAULT);
	}

	public MinuteRushGame(MinecraftServer server, MinuteRushConfig config) {
		this.server = server;
		this.config = config;
		this.arena = new ArenaManager(server);
	}

	public MinecraftServer server() {
		return server;
	}

	public MatchState state() {
		return state;
	}

	public boolean isParticipant(ServerPlayer player) {
		return participants.contains(player.getUUID());
	}

	public int score(UUID playerId) {
		return scores.getOrDefault(playerId, 0);
	}

	public int taskScore() {
		return task == null ? 0 : 1;
	}

	public String taskDescription() {
		return task == null ? "Waiting for a round" : task.description();
	}

	public int timeScore() {
		return Math.max(0, (ticksRemaining + 19) / 20);
	}

	public void setTargetScore(int targetScore) {
		config = config.withTargetScore(targetScore);
	}

	public void setRoundDurationSeconds(int roundDurationSeconds) {
		config = config.withRoundDurationSeconds(roundDurationSeconds);
	}

	public void setDifficulty(com.edwinc125.minuterush.task.MatchDifficulty difficulty) {
		config = config.withDifficulty(difficulty);
	}

	public void setPvp(boolean pvp) {
		config = config.withPvp(pvp);
	}

	public MinuteRushConfig config() {
		return config;
	}

	/**
	 * Whether one match player may hurt another. With PvP off, players can only hurt each other while a task that
	 * targets a player (hit, kill, hook, ...) is active. Outside a match, and for anyone not in it, vanilla rules apply.
	 */
	public boolean allowsPlayerDamage(ServerPlayer target, ServerPlayer attacker) {
		if (config.pvp() || attacker == target || !isMatchInProgress()
				|| !participants.contains(target.getUUID()) || !participants.contains(attacker.getUUID())) {
			return true;
		}
		return state == MatchState.ROUND_ACTIVE && TaskFactory.isPlayerTask(currentTaskId);
	}

	public void playerJoined(ServerPlayer player) {
		scores.putIfAbsent(player.getUUID(), 0);
		if (!com.edwinc125.minuterush.task.TaskFactory.isPlayerTask(currentTaskId) || state != MatchState.ROUND_ACTIVE) {
			// They logged out while glowing from a player task that has since ended.
			clearPlayerInteractionGlow();
		}
		if (state != MatchState.IDLE && state != MatchState.MATCH_OVER) {
			if (participants.contains(player.getUUID())) {
				rejoinMatch(player);
				return;
			}
			arena.addSpectator(player);
			player.sendSystemMessage(Component.literal("A match is in progress; you are now a spectator."));
			return;
		}
		lobbyReady.remove(player.getUUID());
		LobbyItems.apply(player, false, lobbySpectators.contains(player.getUUID()));
		ScoreboardDisplay.refresh(server, this);
		player.sendSystemMessage(Component.literal("Minute Rush: use /minute-rush start when everyone is ready."));
	}

	public boolean isMatchInProgress() {
		return state == MatchState.ROUND_ACTIVE || state == MatchState.COUNTDOWN
			|| state == MatchState.PREPARING_ARENA || state == MatchState.PRE_GAME_COUNTDOWN
			|| state == MatchState.INTERMISSION;
	}

	/** A player who was in the match when it started reconnected: put them back in, not into spectator. */
	private void rejoinMatch(ServerPlayer player) {
		arena.rejoinParticipant(player);
		if (state == MatchState.ROUND_ACTIVE && task != null) {
			// Re-sends the prompt and sets their progress baseline. Without it, a task that started while
			// they were offline would compare lifetime stats (e.g. all jumps ever) and complete instantly.
			task.begin(player);
		}
		player.sendSystemMessage(Component.literal("Welcome back! You have rejoined the match."));
		ScoreboardDisplay.refresh(server, this);
	}

	public void playerLeft(ServerPlayer player) {
		if (isCategoryTest(player)) {
			clearCategoryTest();
		}
		// Participants stay in the match while offline so they can rejoin; only reset once nobody is left.
		// The leaving player may still be in the player list at this point, so exclude them explicitly.
		if (isMatchInProgress() && participants.contains(player.getUUID())
				&& activePlayers().stream().allMatch(other -> other.getUUID().equals(player.getUUID()))) {
			reset("Not enough players remain; match reset.");
		}
		skipVote.remove(player.getUUID());
		if (state == MatchState.ROUND_ACTIVE) {
			checkSkipVotes();
		}
		if (state == MatchState.IDLE) {
			lobbyReady.remove(player.getUUID());
			lobbySpectators.remove(player.getUUID());
			recalculateLobbyCountdown(false);
			ScoreboardDisplay.refresh(server, this);
		}
	}

	private void playSound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
		Holder<net.minecraft.sounds.SoundEvent> holder = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
		player.connection.send(new ClientboundSoundPacket(
			holder, SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(),
			volume, pitch, random.nextLong()));
	}

	public void tick() {
		if (cleanupPending) {
			if (arena.tickCleanup()) {
				cleanupPending = false;
				bossbar.removeAllPlayers();
				bossbar.setVisible(false);
				ScoreboardDisplay.clear(server);
				lobbyReady.clear();
				lobbySpectators.clear();
				refreshLobbyPlayers();
			}
			return;
		}
		tickCategoryTest();
		arena.tickModifiers();
		if (state == MatchState.MATCH_OVER) {
			tickMatchOver();
			return;
		}
		if (state == MatchState.IDLE) {
			tickLobby();
			return;
		}
		if (state == MatchState.PREPARING_ARENA) {
			arena.tickPreparation();
			updatePreparationPresentation();
			if (arena.ready()) {
				arena.placeParticipants(config);
				state = MatchState.PRE_GAME_COUNTDOWN;
				countdownTicks = config.preGameCountdownSeconds() * 20;
				title("Arena ready", "Get ready!");
				playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0f, 0.8f);
			}
			return;
		}
		if (state == MatchState.ROUND_ACTIVE || state == MatchState.INTERMISSION || state == MatchState.COUNTDOWN) {
			matchTicks++;
		}
		if (state == MatchState.PRE_GAME_COUNTDOWN || state == MatchState.COUNTDOWN) {
			arena.holdFrozen();
			playCountdownCue(countdownTicks);
			if (--countdownTicks <= 0) {
				arena.release();
				playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 1.6f);
				state = MatchState.ROUND_ACTIVE;
				startRound();
			} else {
				updatePresentation();
			}
			return;
		}
		if (state == MatchState.INTERMISSION) {
			if (--intermissionTicks <= 0) {
				state = MatchState.ROUND_ACTIVE;
				startRound();
			} else {
				updateIntermissionPresentation();
			}
			return;
		}
		if (state != MatchState.ROUND_ACTIVE) {
			return;
		}
		if (--ticksRemaining <= 0) {
			server.getPlayerList().broadcastSystemMessage(Component.literal("Time's up! The round is skipped."), false);
			beginIntermission("No one completed the task! Skipping...");
			playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 0.9f, 0.7f);
			return;
		}
		arena.expandBorder(config);
		updatePlayerInteractionGlow();
		updatePresentation();
		// Check everyone before awarding, so all players who finish on the same tick get a point.
		List<ServerPlayer> winners = new java.util.ArrayList<>();
		for (ServerPlayer player : activePlayers()) {
			trackStandingBlock(player);
			if (player.fishing != null && player.fishing.getHookedIn() instanceof ServerPlayer hooked && hooked != player) {
				com.edwinc125.minuterush.task.EntityTaskProgress.record(player.getUUID(), "hook:player");
			}
			if (task.isComplete(player)) {
				winners.add(player);
			}
		}
		if (!winners.isEmpty()) {
			winRound(winners);
		}
	}

	public String start() {
		clearCategoryTest();
		if (state == MatchState.MATCH_OVER) {
			return "The match is ending; try again once everyone is back in the lobby.";
		}
		if (state != MatchState.IDLE) {
			return "A match is already in progress.";
		}
		participants.clear();
		recentTaskIds.clear();
		for (ServerPlayer player : lobbyPlayers()) {
			participants.add(player.getUUID());
		}
		if (participants.isEmpty()) {
			return "No players are participating in this match.";
		}
		return beginMatch("Minute Rush started!");
	}

	public String startSolo(ServerPlayer player) {
		clearCategoryTest();
		lobbyReady.clear();
		lobbySpectators.clear();
		lobbyCountdownTicks = 0;
		participants.clear();
		participants.add(player.getUUID());
		return beginMatch("Singleplayer test mode started. Use /minute-rush solo-exit to stop.");
	}

	private String beginMatch(String message) {
		lobbyCountdownTicks = 0;
		scores.replaceAll((id, score) -> 0);
		recentTaskIds.clear();
		difficultyProgress.reset();
		matchTicks = 0;
		intermissionTicks = 0;
		currentTaskId = null;
		state = MatchState.PREPARING_ARENA;
		title("Generating Arena...", "Please wait");
		arena.beginPreparation(activePlayers().stream().toList(), lobbySpectatorPlayers(), config);
		lobbyReady.clear();
		ScoreboardDisplay.refresh(server, this);
		updatePresentation();
		return message;
	}

	public String reset(String message) {
		clearCategoryTest();
		clearPlayerInteractionGlow();
		arena.restoreLobbyState();
		cleanupPending = arena.cleanupActive();
		state = MatchState.IDLE;
		task = null;
		recentTaskIds.clear();
		ticksRemaining = 0;
		countdownTicks = 0;
		intermissionTicks = 0;
		currentTaskId = null;
		skipVote.clear();
		participants.clear();
		lobbyReady.clear();
		lobbySpectators.clear();
		lobbyCountdownTicks = 0;
		scores.replaceAll((id, score) -> 0);
		server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
		if (!cleanupPending) {
			ScoreboardDisplay.clear(server);
			bossbar.removeAllPlayers();
			bossbar.setVisible(false);
			refreshLobbyPlayers();
		}
		return message;
	}

	public String skipRound() {
		if (state != MatchState.ROUND_ACTIVE && state != MatchState.COUNTDOWN
				&& state != MatchState.PRE_GAME_COUNTDOWN && state != MatchState.INTERMISSION) {
			return "No round is active.";
		}
		skipCurrentRound("Round skipped by operator.");
		return "Round skipped.";
	}

	public String voteSkip(ServerPlayer player) {
		if (state != MatchState.ROUND_ACTIVE) {
			return "There is no active task to vote on.";
		}
		List<UUID> eligible = eligibleVoters();
		int needed = SkipVote.required(eligible.size());
		return switch (skipVote.vote(player.getUUID(), eligible)) {
			case NOT_ELIGIBLE -> "Only players in the current match can vote to skip.";
			case ALREADY_VOTED -> "You already voted to skip (" + skipVote.count() + "/" + needed + ").";
			case ACCEPTED -> {
				server.getPlayerList().broadcastSystemMessage(Component.literal(
					player.getGameProfile().name() + " voted to skip the task (" + skipVote.count() + "/" + needed + "). ")
						.append(voteSkipButton()), false);
				yield VOTE_RECORDED;
			}
			case PASSED -> {
				skipCurrentRound("Vote passed: task skipped.");
				yield VOTE_RECORDED;
			}
		};
	}

	private static Component voteSkipButton() {
		return Component.literal("[Vote to skip]").withStyle(style -> style
			.withColor(ChatFormatting.YELLOW)
			.withClickEvent(new ClickEvent.RunCommand("/voteskip"))
			.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to vote to skip this task"))));
	}

	private void checkSkipVotes() {
		if (skipVote.passes(eligibleVoters())) {
			skipCurrentRound("Vote passed: task skipped.");
		}
	}

	private List<UUID> eligibleVoters() {
		return activePlayers().stream().map(ServerPlayer::getUUID).toList();
	}

	private void skipCurrentRound(String reason) {
		skipVote.clear();
		server.getPlayerList().broadcastSystemMessage(Component.literal(reason), false);
		beginIntermission(reason);
	}

	public boolean isCategoryTest(ServerPlayer player) {
		return testPlayer != null && testPlayer.getUUID().equals(player.getUUID());
	}

	public String startCategoryTest(ServerPlayer player, String category) {
		List<TaskFactory.Selection> selections = TaskFactory.category(server.overworld(), category);
		if (selections.isEmpty()) {
			return "Unknown or empty test category: " + category;
		}
		clearCategoryTest();
		testPlayer = player;
		testCategory = category;
		testSelections = selections;
		testIndex = 0;
		assignTestTask();
		return "Category test started: " + category + ".";
	}

	public String skipTestTask(ServerPlayer player) {
		if (!isCategoryTest(player)) {
			return "No category test is active for this player.";
		}
		advanceTestTask();
		return "Test task skipped.";
	}

	public String status() {
		String mode = participants.size() == 1 ? "singleplayer test" : "multiplayer";
		return "State: " + state + " (" + mode + "), target score: " + config.targetScore() + ", round seconds: " + config.roundDurationSeconds() + ", difficulty: " + difficultyDescription();
	}

	private String difficultyDescription() {
		if (config.difficulty() != com.edwinc125.minuterush.task.MatchDifficulty.PROGRESSIVE) {
			return config.difficulty().id();
		}
		return isMatchInProgress()
			? "progressive (now " + difficultyProgress.progressivePhase().id() + ")"
			: "progressive";
	}

	private void startRound() {
		if (state != MatchState.ROUND_ACTIVE) return;
		TaskFactory.Selection selection = TaskFactory.next(server.overworld(), random, Set.copyOf(recentTaskIds),
			activePlayers().size() > 1, arena::taskAvailable,
			// N counts everyone in the match, including anyone briefly disconnected (they can rejoin). Counting only
			// online players would let one dropped connection end the warm-up early, with no way back.
			difficultyProgress.next(config.difficulty(), participants.size(), matchTicks));
		task = selection.task();
		currentTaskId = selection.id();
		skipVote.clear();
		recentTaskIds.addLast(selection.id());
		while (recentTaskIds.size() > 3) {
			recentTaskIds.removeFirst();
		}

		ticksRemaining = config.roundDurationSeconds() * 20;
		server.getPlayerList().broadcastSystemMessage(Component.literal("New round: " + task.description() + " ")
			.append(voteSkipButton()), false);
		title("NEW TASK", task.description());
		playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 1.2f);
		for (ServerPlayer player : activePlayers()) task.begin(player);
		for (ServerPlayer player : activePlayers()) {
			previousStandingBlocks.put(player.getUUID(), standingBlock(player));
		}
		ScoreboardDisplay.refresh(server, this);
		updatePresentation();
	}

	private void tickCategoryTest() {
		if (testPlayer != null && testTask != null && testTask.isComplete(testPlayer)) {
			advanceTestTask();
		}
	}

	private void advanceTestTask() {
		testIndex = (testIndex + 1) % testSelections.size();
		assignTestTask();
	}

	private void assignTestTask() {
		testTask = testSelections.get(testIndex).task();
		testTask.begin(testPlayer);
		testPlayer.sendSystemMessage(Component.literal(
			"Category test [" + testCategory + "] " + (testIndex + 1) + "/" + testSelections.size()
				+ ": " + testTask.description()).withStyle(ChatFormatting.AQUA));
	}

	private void clearCategoryTest() {
		testPlayer = null;
		testCategory = null;
		testSelections = List.of();
		testIndex = 0;
		testTask = null;
	}

	private void beginIntermission(String message) {
		state = MatchState.INTERMISSION;
		intermissionMessage = message;
		intermissionTicks = config.interRoundBreakSeconds() * 20;
		lastIntermissionSecond = -1;
		clearPlayerInteractionGlow();
		title("Round complete", message);
		playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0f, 0.8f);
		updatePresentation();
	}

	private void playCountdownCue(int ticks) {
		if (ticks <= 0 || ticks % 20 != 0) {
			return;
		}
		int seconds = Math.max(1, (ticks + 19) / 20);
		playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0f, countdownPitch(seconds));
	}

	private static float countdownPitch(int seconds) {
		return switch (seconds) {
			case 1 -> 1.2f;
			case 2 -> 0.9f;
			default -> 0.6f;
		};
	}

	private void updateIntermissionPresentation() {
		int elapsed = config.interRoundBreakSeconds() * 20 - intermissionTicks;
		int second = Math.max(1, (intermissionTicks + 19) / 20);
		if (elapsed >= 40 && second != lastIntermissionSecond) {
			int seconds = Math.max(1, Math.min(3, (intermissionTicks + 19) / 20));
			title(Integer.toString(seconds), "Next round");
			if (second != lastIntermissionSecond) {
				playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0f,
					countdownPitch(seconds));
			}
			lastIntermissionSecond = second;
		} else if (elapsed < 40 && lastIntermissionSecond != 0) {
			title("Round complete", intermissionMessage);
			lastIntermissionSecond = 0;
		}
		updatePresentation();
	}

	public void blockUsed(ServerPlayer player, net.minecraft.core.BlockPos pos) {
		if (state == MatchState.ROUND_ACTIVE && task instanceof com.edwinc125.minuterush.task.InteractionTask interaction) {
			String block = BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(pos).getBlock()).getPath();
			interaction.record(player, block);
		}
	}

	private void trackStandingBlock(ServerPlayer player) {
		String current = standingBlock(player);
		String previous = previousStandingBlocks.put(player.getUUID(), current);
		if (state == MatchState.ROUND_ACTIVE
				&& task instanceof com.edwinc125.minuterush.task.InteractionTask interaction
				&& interaction.countsStandingOn()
				&& interaction.accepts(current)
				&& (previous == null || !interaction.accepts(previous))) {
			interaction.record(player, current);
		}
	}

	private static String standingBlock(ServerPlayer player) {
		return BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(player.getOnPos()).getBlock()).getPath();
	}

	public void entityUsed(ServerPlayer player, String target) {
		if (state == MatchState.ROUND_ACTIVE && task instanceof com.edwinc125.minuterush.task.InteractionTask interaction) {
			interaction.record(player, target);
		}
	}

	private void title(String title, String subtitle) {
		for (ServerPlayer player : activePlayers()) {
			player.connection.send(new ClientboundSetTitlesAnimationPacket(
				TitleTimings.FADE_IN_TICKS, TitleTimings.STAY_TICKS, TitleTimings.FADE_OUT_TICKS));
			player.connection.send(new ClientboundSetTitleTextPacket(
				Component.literal(title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
			player.connection.send(new ClientboundSetSubtitleTextPacket(
				Component.literal(subtitle).withStyle(ChatFormatting.WHITE)));
		}
	}

	private void playSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
		Holder<net.minecraft.sounds.SoundEvent> holder = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
		for (ServerPlayer player : activePlayers()) {
			player.connection.send(new ClientboundSoundPacket(
				holder, SoundSource.MASTER, player.getX(), player.getY(), player.getZ(),
				volume, pitch, random.nextLong()));
		}
	}

	private void updatePresentation() {
		// Everyone online sees the bossbar, including spectators who are not on the scoreboard.
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			bossbar.addPlayer(player);
		}
		bossbar.setVisible(state == MatchState.COUNTDOWN || state == MatchState.PRE_GAME_COUNTDOWN
			|| state == MatchState.ROUND_ACTIVE || state == MatchState.INTERMISSION
			|| state == MatchState.MATCH_OVER);
		if (state == MatchState.ROUND_ACTIVE && ticksRemaining > 0) {
			bossbar.setName(Component.literal(task.description() + "  |  " + timeScore() + "s")
				.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
			bossbar.setProgress(Math.max(0.0f, Math.min(1.0f,
				(float) ticksRemaining / (config.roundDurationSeconds() * 20.0f))));
		} else if (state == MatchState.COUNTDOWN || state == MatchState.PRE_GAME_COUNTDOWN) {
			bossbar.setName(Component.literal("Next round in " + Math.max(1, (countdownTicks + 19) / 20) + "s")
				.withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
			bossbar.setProgress(Math.max(0.0f, Math.min(1.0f,
				countdownTicks / (float) (config.preGameCountdownSeconds() * 20))));
		} else if (state == MatchState.INTERMISSION) {
			bossbar.setName(Component.literal("Next round in " + Math.max(1, (intermissionTicks + 19) / 20) + "s")
				.withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
			bossbar.setProgress(Math.max(0.0f, Math.min(1.0f,
				intermissionTicks / (float) (config.interRoundBreakSeconds() * 20))));
		} else if (state == MatchState.MATCH_OVER) {
			bossbar.setName(Component.literal("Returning to lobby in " + Math.max(1, (matchOverTicks + 19) / 20) + "s")
				.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
			bossbar.setProgress(Math.max(0.0f, Math.min(1.0f,
				matchOverTicks / (float) (MATCH_END_DELAY_SECONDS * 20))));
		}
		ScoreboardDisplay.refresh(server, this);
	}

	private void tickMatchOver() {
		if (--matchOverTicks > 0) {
			updatePresentation();
			return;
		}
		arena.restoreLobbyState();
		cleanupPending = arena.cleanupActive();
		state = MatchState.IDLE;
	}

	/** Awards a point to every player who completed the task this tick. */
	private void winRound(List<ServerPlayer> winners) {
		difficultyProgress.taskCompleted();
		List<String> names = new java.util.ArrayList<>();
		List<String> scoreParts = new java.util.ArrayList<>();
		List<String> matchWinners = new java.util.ArrayList<>();
		for (ServerPlayer winner : winners) {
			int newScore = score(winner.getUUID()) + 1;
			scores.put(winner.getUUID(), newScore);
			String name = winner.getGameProfile().name();
			names.add(name);
			scoreParts.add(winners.size() == 1 ? newScore + "/" + config.targetScore()
				: name + " " + newScore + "/" + config.targetScore());
			if (newScore >= config.targetScore()) {
				matchWinners.add(name);
			}
		}
		String who = WinnerNames.join(names) + " " + WinnerNames.verb(names);
		server.getPlayerList().broadcastSystemMessage(Component.literal(
			who + " the round! (" + String.join(", ", scoreParts) + ")"), false);
		title(who + "!", winners.size() == 1 ? "Round score: " + scoreParts.get(0) : "+1 point each");
		playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.4f);
		playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
		ScoreboardDisplay.refresh(server, this);
		if (!matchWinners.isEmpty()) {
			// Stay in the arena for a few seconds before returning everyone to the lobby (see tickMatchOver).
			String matchWho = WinnerNames.join(matchWinners) + " " + WinnerNames.verb(matchWinners);
			state = MatchState.MATCH_OVER;
			matchOverTicks = MATCH_END_DELAY_SECONDS * 20;
			task = null;
			playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
			title(matchWho + " the match!", "Returning to lobby in " + MATCH_END_DELAY_SECONDS + "s");
			server.getPlayerList().broadcastSystemMessage(Component.literal(
				matchWho + " the match! Use /minute-rush start to play again."), false);
			clearPlayerInteractionGlow();
			updatePresentation();
		} else {
			beginIntermission(who + " the round!");
		}
	}

	public void playerDied(ServerPlayer player) {
		if (state == MatchState.ROUND_ACTIVE && isDeathTask()) {
			arena.enableKeepInventory(config.keepInventoryGraceSeconds());
		}
	}

	private boolean isDeathTask() {
		return currentTaskId != null && (currentTaskId.startsWith("kill-")
			|| currentTaskId.endsWith("-damage") || currentTaskId.equals("fall-damage"));
	}

	/**
	 * During any task that involves another player, everyone glows so they can find each other, and
	 * keepInventory stays on (until the grace period after the round) so PvP deaths don't cost items.
	 */
	private void updatePlayerInteractionGlow() {
		if (!com.edwinc125.minuterush.task.TaskFactory.isPlayerTask(currentTaskId)) {
			clearPlayerInteractionGlow();
			return;
		}
		arena.enableKeepInventory(config.keepInventoryGraceSeconds());
		for (ServerPlayer player : activePlayers()) {
			// Use the glowing tag, not the Glowing effect, so the player's own effects (e.g. a glowing
			// potion or spectral arrow) are never overwritten or removed. Players who already had the
			// tag from elsewhere are left alone and not tracked.
			if (!player.hasGlowingTag()) {
				player.setGlowingTag(true);
				glowTagged.add(player.getUUID());
			}
		}
	}

	/** Removes only the glowing tag this game set; players who are offline are cleared when they rejoin. */
	private void clearPlayerInteractionGlow() {
		if (glowTagged.isEmpty()) {
			return;
		}
		glowTagged.removeIf(id -> {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player == null) {
				return false;
			}
			player.setGlowingTag(false);
			return true;
		});
	}

	private void updatePreparationPresentation() {
		// Everyone online sees the bossbar, including spectators who are not on the scoreboard.
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			bossbar.addPlayer(player);
		}
		bossbar.setVisible(true);
		bossbar.setName(Component.literal("Generating Arena... " + arena.generatedChunks() + "/" + arena.totalChunks())
			.withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
		bossbar.setProgress(arena.totalChunks() == 0 ? 0.0f
			: Math.min(1.0f, arena.generatedChunks() / (float) arena.totalChunks()));
	}

	public boolean handleLobbyItemUse(ServerPlayer player, net.minecraft.world.InteractionHand hand) {
		if (state != MatchState.IDLE) {
			return false;
		}
		LobbyItems.Action action = LobbyItems.action(player.getItemInHand(hand));
		if (action == LobbyItems.Action.NONE) {
			return false;
		}
		switch (action) {
			case READY -> {
				if (lobbySpectators.contains(player.getUUID())) return true;
				if (lobbyReady.add(player.getUUID())) {
					broadcastLobbyMessage(player.getGameProfile().name() + " is ready! ("
						+ readyLobbyCount() + "/" + lobbyPlayers().size() + ")");
					recalculateLobbyCountdown(false);
				}
			}
			case UNREADY -> {
				if (lobbyReady.remove(player.getUUID())) {
					broadcastLobbyMessage(player.getGameProfile().name() + " is no longer ready. ("
						+ readyLobbyCount() + "/" + lobbyPlayers().size() + ")");
					recalculateLobbyCountdown(true);
				}
			}
			case SPECTATE -> {
				lobbyReady.remove(player.getUUID());
				if (lobbySpectators.add(player.getUUID())) {
					broadcastLobbyMessage(player.getGameProfile().name() + " wants to sit this game out!");
					recalculateLobbyCountdown(false);
				}
			}
			case JOIN -> {
				if (lobbySpectators.remove(player.getUUID())) {
					lobbyReady.remove(player.getUUID());
					broadcastLobbyMessage(player.getGameProfile().name()
						+ " has changed their mind and wanted to join this game!");
					recalculateLobbyCountdown(false);
				}
			}
			default -> {
			}
		}
		LobbyItems.apply(player, lobbyReady.contains(player.getUUID()), lobbySpectators.contains(player.getUUID()));
		ScoreboardDisplay.refresh(server, this);
		playSound(player, SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f);
		return true;
	}

	public boolean isLobbySpectator(ServerPlayer player) {
		return state == MatchState.IDLE && lobbySpectators.contains(player.getUUID());
	}

	public boolean isLobby() {
		return state == MatchState.IDLE;
	}

	public List<String> lobbyPlayerNames() {
		return lobbyPlayers().stream().map(player -> player.getGameProfile().name()).toList();
	}

	public List<String> lobbySpectatorNames() {
		return lobbySpectatorPlayers().stream().map(player -> player.getGameProfile().name()).toList();
	}

	private void tickLobby() {
		refreshLobbyPlayers();
		removeDroppedLobbyItems();
		if (lobbyCountdownTicks > 0) {
			if (--lobbyCountdownTicks <= 0) {
				start();
				return;
			}
			updateLobbyPresentation();
		} else {
			bossbar.removeAllPlayers();
			bossbar.setVisible(false);
			ScoreboardDisplay.refresh(server, this);
		}
	}

	private void removeDroppedLobbyItems() {
		for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
			for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
				if (entity instanceof net.minecraft.world.entity.item.ItemEntity item
						&& LobbyItems.isControl(item.getItem())) {
					item.discard();
				}
			}
		}
	}

	private void updateLobbyPresentation() {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			bossbar.addPlayer(player);
		}
		bossbar.setVisible(lobbyCountdownTicks > 0);
		bossbar.setName(Component.literal(LobbyCountdownPolicy.bossbarText(lobbyCountdownTicks))
			.withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
		bossbar.setProgress(Math.max(0.0f, Math.min(1.0f,
			lobbyCountdownTicks / (float) LobbyCountdownPolicy.INITIAL_TICKS)));
		ScoreboardDisplay.refresh(server, this);
	}

	private void recalculateLobbyCountdown(boolean readyRemoved) {
		int active = lobbyPlayers().size();
		int ready = readyLobbyCount();
		if (active == 0 || ready == 0) {
			lobbyCountdownTicks = 0;
			return;
		}
		if (lobbyCountdownTicks == 0) {
			lobbyCountdownTicks = LobbyCountdownPolicy.INITIAL_TICKS;
		}
		if (readyRemoved && ready < LobbyCountdownPolicy.requiredReady(active)) {
			lobbyCountdownTicks += LobbyCountdownPolicy.EXTENSION_TICKS;
		} else if (ready == active) {
			lobbyCountdownTicks = Math.min(lobbyCountdownTicks, LobbyCountdownPolicy.ALL_READY_TICKS);
		} else if (ready >= LobbyCountdownPolicy.requiredReady(active)) {
			lobbyCountdownTicks = Math.min(lobbyCountdownTicks, LobbyCountdownPolicy.MAJORITY_TICKS);
		}
		updateLobbyPresentation();
	}

	private int readyLobbyCount() {
		Set<UUID> active = lobbyPlayers().stream().map(ServerPlayer::getUUID).collect(java.util.stream.Collectors.toSet());
		return (int) lobbyReady.stream().filter(active::contains).count();
	}

	private void refreshLobbyPlayers() {
		if (state != MatchState.IDLE) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			LobbyItems.apply(player, lobbyReady.contains(player.getUUID()), lobbySpectators.contains(player.getUUID()));
		}
		ScoreboardDisplay.refresh(server, this);
	}

	private void broadcastLobbyMessage(String message) {
		server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
	}

	private List<ServerPlayer> lobbyPlayers() {
		return server.getPlayerList().getPlayers().stream()
			.filter(player -> !lobbySpectators.contains(player.getUUID()))
			.toList();
	}

	private List<ServerPlayer> lobbySpectatorPlayers() {
		return server.getPlayerList().getPlayers().stream()
			.filter(player -> lobbySpectators.contains(player.getUUID()))
			.toList();
	}

	private java.util.Collection<ServerPlayer> activePlayers() {
		return server.getPlayerList().getPlayers().stream()
			.filter(player -> participants.contains(player.getUUID()))
			.toList();
	}
}
