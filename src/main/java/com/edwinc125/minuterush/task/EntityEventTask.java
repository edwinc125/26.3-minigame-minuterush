package com.edwinc125.minuterush.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class EntityEventTask implements MinuteRushTask {
	private final String event;
	private final String prompt;
	private final Map<UUID, Integer> baselines = new HashMap<>();

	public EntityEventTask(String event, String prompt) {
		this.event = event;
		this.prompt = prompt;
	}

	@Override
	public String description() {
		return prompt;
	}

	@Override
	public void begin(ServerPlayer player) {
		baselines.put(player.getUUID(), EntityTaskProgress.count(player.getUUID(), event));
		player.sendSystemMessage(Component.literal(prompt).withStyle(net.minecraft.ChatFormatting.RED));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		return EntityTaskProgress.count(player.getUUID(), event)
			> baselines.getOrDefault(player.getUUID(), 0);
	}
}
