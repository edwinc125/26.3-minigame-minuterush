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
		for (var entry : scoreboard.listPlayerScores(objective)) {
			if (entry.owner().startsWith("§bTask:") || entry.owner().startsWith("§eTime:")) {
				scoreboard.resetSinglePlayerScore(ScoreHolder.forNameOnly(entry.owner()), objective);
			}
		}
		String task = game.taskDescription();
		scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly("§bTask: " + task), objective).set(0);
		scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly("§eTime: " + game.timeScore() + "s"), objective).set(0);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			scoreboard.getOrCreatePlayerScore(ScoreHolder.fromGameProfile(player.getGameProfile()), objective)
				.set(game.score(player.getUUID()));
		}
	}

	public static void clear(MinecraftServer server) {
		Objective objective = server.getScoreboard().getObjective(OBJECTIVE);
		if (objective != null) {
			server.getScoreboard().removeObjective(objective);
		}
	}
}
