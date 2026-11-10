package com.edwinc125.minuterush.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class CrouchTask implements MinuteRushTask {
	private final int target;
	private final Map<UUID, CrouchProgress> progress = new HashMap<>();

	public CrouchTask(int target) {
		if (target <= 0) throw new IllegalArgumentException("Crouch target must be positive");
		this.target = target;
	}

	@Override
	public String description() {
		return "Crouch " + target + " times";
	}

	@Override
	public void begin(ServerPlayer player) {
		UUID id = player.getUUID();
		progress.put(id, new CrouchProgress(target, player.isCrouching()));
		player.sendSystemMessage(Component.literal(description()).withStyle(net.minecraft.ChatFormatting.BLUE));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		UUID id = player.getUUID();
		CrouchProgress state = progress.get(id);
		return state != null && state.update(player.isCrouching());
	}
}
