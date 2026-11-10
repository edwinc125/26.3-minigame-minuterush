package com.edwinc125.minuterush.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ChatTask implements MinuteRushTask {
	private final String prompt;
	private final java.util.function.Predicate<String> matcher;
	private final Map<UUID, Boolean> completed = new HashMap<>();

	public ChatTask(String prompt, java.util.function.Predicate<String> matcher) {
		this.prompt = prompt;
		this.matcher = matcher;
	}

	@Override
	public String description() {
		return prompt;
	}

	@Override
	public void begin(ServerPlayer player) {
		begin(player.getUUID());
		player.sendSystemMessage(Component.literal(prompt).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
	}

	public void accept(ServerPlayer player, String message) {
		accept(player.getUUID(), message);
	}

	void begin(UUID player) {
		completed.put(player, false);
	}

	void accept(UUID player, String message) {
		if (completed.containsKey(player) && matches(message)) {
			completed.put(player, true);
		}
	}

	boolean matches(String message) {
		return matcher.test(message.trim());
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		return completed(player.getUUID());
	}

	boolean completed(UUID player) {
		return completed.getOrDefault(player, false);
	}
}
