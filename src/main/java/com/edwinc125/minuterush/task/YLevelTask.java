package com.edwinc125.minuterush.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class YLevelTask implements MinuteRushTask {
	private final int level;
	private final Map<UUID, YLevelProgress> progress = new HashMap<>();

	public YLevelTask(int level) {
		this.level = level;
	}

	@Override
	public String description() {
		return "Reach Y = " + level;
	}

	@Override
	public void begin(ServerPlayer player) {
		progress.put(player.getUUID(), new YLevelProgress(level, player.getY()));
		player.sendSystemMessage(Component.literal(description()).withStyle(net.minecraft.ChatFormatting.BLUE));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		return progress.computeIfAbsent(player.getUUID(),
			ignored -> new YLevelProgress(level, player.getY())).update(player.getY());
	}
}
