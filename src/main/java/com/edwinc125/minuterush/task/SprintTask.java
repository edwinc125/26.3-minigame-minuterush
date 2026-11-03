package com.edwinc125.minuterush.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SprintTask implements MinuteRushTask {
	private final int seconds;
	private final Map<UUID, Integer> ticks = new HashMap<>();

	public SprintTask(int seconds) {
		this.seconds = seconds;
	}

	@Override
	public String description() {
		return "Sprint continuously for " + seconds + " seconds";
	}

	@Override
	public void begin(ServerPlayer player) {
		ticks.put(player.getUUID(), 0);
		player.sendSystemMessage(Component.literal(description()).withStyle(net.minecraft.ChatFormatting.YELLOW));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		int current = player.isSprinting() ? ticks.getOrDefault(player.getUUID(), 0) + 1 : 0;
		ticks.put(player.getUUID(), current);
		return current >= seconds * 20;
	}
}
