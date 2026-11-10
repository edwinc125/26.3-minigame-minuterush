package com.edwinc125.minuterush.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class LookTask implements MinuteRushTask {
	private final boolean up;
	private final Map<UUID, Boolean> complete = new HashMap<>();

	public LookTask(boolean up) {
		this.up = up;
	}

	@Override
	public String description() {
		return up ? "Look straight up" : "Look straight down";
	}

	@Override
	public void begin(ServerPlayer player) {
		complete.put(player.getUUID(), false);
		player.sendSystemMessage(Component.literal(description()).withStyle(net.minecraft.ChatFormatting.BLUE));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		boolean done = matches(player.getXRot(), up);
		if (done) complete.put(player.getUUID(), true);
		return complete.getOrDefault(player.getUUID(), false);
	}

	static boolean matches(float pitch, boolean up) {
		return up ? pitch < -89.0f : pitch > 89.0f;
	}
}
