package com.edwinc125.minuterush.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class StatTask implements MinuteRushTask {
	private final String prompt;
	private final int required;
	private final ToIntFunction<ServerPlayer> value;
	private final Map<UUID, Integer> baselines = new HashMap<>();

	public StatTask(String prompt, int required, ToIntFunction<ServerPlayer> value) {
		this.prompt = prompt;
		this.required = required;
		this.value = value;
	}

	@Override
	public String description() {
		return prompt;
	}

	@Override
	public void begin(ServerPlayer player) {
		baselines.put(player.getUUID(), value.applyAsInt(player));
		player.sendSystemMessage(Component.literal(prompt).withStyle(net.minecraft.ChatFormatting.YELLOW));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		return value.applyAsInt(player) - baselines.getOrDefault(player.getUUID(), 0) >= required;
	}
}
