package com.edwinc125.minuterush.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class JumpTask implements MinuteRushTask {
	private final Map<UUID, JumpProgress> progress = new HashMap<>();

	@Override
	public String description() {
		return "Jump once";
	}

	@Override
	public void begin(ServerPlayer player) {
		UUID id = player.getUUID();
		progress.put(id, new JumpProgress(player.onGround()));
		player.sendSystemMessage(net.minecraft.network.chat.Component.literal(description())
			.withStyle(net.minecraft.ChatFormatting.GREEN));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		UUID id = player.getUUID();
		JumpProgress jumpProgress = progress.get(id);
		if (jumpProgress == null) {
			begin(player);
			jumpProgress = progress.get(id);
		}
		return jumpProgress.update(player.onGround());
	}
}
