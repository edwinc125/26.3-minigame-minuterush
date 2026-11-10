package com.edwinc125.minuterush.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class DeathTask implements MinuteRushTask {
	private final Map<UUID, Integer> baselines = new HashMap<>();
	private final String event;
	private final String prompt;

	public DeathTask(String event, String prompt) {
		this.event = event;
		this.prompt = prompt;
	}

	@Override
	public String description() {
		return prompt;
	}

	@Override
	public void begin(ServerPlayer player) {
		begin(player.getUUID());
		player.sendSystemMessage(Component.literal(description()).withStyle(net.minecraft.ChatFormatting.DARK_RED));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		return isComplete(player.getUUID());
	}

	void begin(UUID player) {
		baselines.put(player, EntityTaskProgress.count(player, event));
	}

	boolean isComplete(UUID player) {
		return baselines.containsKey(player)
			&& EntityTaskProgress.count(player, event) > baselines.get(player);
	}
}
