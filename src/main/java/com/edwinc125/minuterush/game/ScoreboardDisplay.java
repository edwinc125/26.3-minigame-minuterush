package com.edwinc125.minuterush.game;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ScoreboardDisplay {
	private static final String OBJECTIVE = "minute_rush";

	private ScoreboardDisplay() {}

	public static void refresh(MinecraftServer server, MinuteRushGame game) {
		ServerScoreboard scoreboard = server.getScoreboard();
		Objective objective = scoreboard.getObjective(OBJECTIVE);
		if (objective == null) {
			objective = scoreboard.addObjective(OBJECTIVE, ObjectiveCriteria.DUMMY,
				Component.literal("MINUTE RUSH").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
				ObjectiveCriteria.RenderType.INTEGER, false, null);
			scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
		}
		List<ServerPlayer> listed = server.getPlayerList().getPlayers().stream()
			.filter(player -> shouldList(game.isParticipant(player), player.isSpectator()))
			.toList();
		Set<String> listedNames = new HashSet<>();
		for (ServerPlayer player : listed) {
			listedNames.add(ScoreHolder.fromGameProfile(player.getGameProfile()).getScoreboardName());
		}
		List<String> owners = new ArrayList<>();
		for (var entry : scoreboard.listPlayerScores(objective)) {
			owners.add(entry.owner());
		}
		for (String owner : entriesToReset(owners, listedNames)) {
			scoreboard.resetSinglePlayerScore(ScoreHolder.forNameOnly(owner), objective);
		}
		String task = game.taskDescription();
		scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly("§bTask: " + task), objective).set(0);
		scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly("§eTime: " + game.timeScore() + "s"), objective).set(0);
		for (ServerPlayer player : listed) {
			scoreboard.getOrCreatePlayerScore(ScoreHolder.fromGameProfile(player.getGameProfile()), objective)
				.set(game.score(player.getUUID()));
		}
	}

	/** Only match participants who are not in spectator mode get a line on the sidebar. */
	static boolean shouldList(boolean participant, boolean spectator) {
		return participant && !spectator;
	}

	/**
	 * Sidebar entries to clear before redrawing: the task and time lines (always rewritten)
	 * and any player line for someone who should no longer be listed, e.g. a new spectator.
	 */
	static List<String> entriesToReset(Collection<String> owners, Set<String> listedNames) {
		List<String> reset = new ArrayList<>();
		for (String owner : owners) {
			if (owner.startsWith("§bTask:") || owner.startsWith("§eTime:") || !listedNames.contains(owner)) {
				reset.add(owner);
			}
		}
		return reset;
	}

	public static void clear(MinecraftServer server) {
		Objective objective = server.getScoreboard().getObjective(OBJECTIVE);
		if (objective != null) {
			server.getScoreboard().removeObjective(objective);
		}
	}
}
