package com.edwinc125.minuterush.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class YLevelTask implements MinuteRushTask {
	private final boolean atLeast;
	private final int level;
	private final Map<UUID, Double> starts = new HashMap<>();

	public YLevelTask(boolean atLeast, int level) {
		this.atLeast = atLeast;
		this.level = level;
	}

	@Override
	public String description() {
		return (atLeast ? "Reach Y " : "Reach Y ") + (atLeast ? ">=" : "<=") + level;
	}

	@Override
	public void begin(ServerPlayer player) {
		starts.put(player.getUUID(), player.getY());
		player.sendSystemMessage(Component.literal(description()).withStyle(net.minecraft.ChatFormatting.BLUE));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		double start = starts.getOrDefault(player.getUUID(), player.getY());
		return atLeast ? start < level && player.getY() >= level
			: start > level && player.getY() <= level;
	}
}
