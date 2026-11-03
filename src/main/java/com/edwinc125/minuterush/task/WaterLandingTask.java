package com.edwinc125.minuterush.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;

public final class WaterLandingTask implements MinuteRushTask {
	private final Map<UUID, Boolean> falling = new HashMap<>();
	private final Map<UUID, Boolean> previouslyInFluid = new HashMap<>();
	private final Map<UUID, Double> startY = new HashMap<>();

	@Override
	public String description() {
		return "Fall at least 3 blocks and land in water";
	}

	@Override
	public void begin(ServerPlayer player) {
		falling.put(player.getUUID(), false);
		previouslyInFluid.put(player.getUUID(), inFluid(player));
		startY.put(player.getUUID(), player.getY());
		player.sendSystemMessage(Component.literal(description()).withStyle(net.minecraft.ChatFormatting.BLUE));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		UUID id = player.getUUID();
		boolean inFluid = inFluid(player);
		boolean wasInFluid = previouslyInFluid.getOrDefault(id, inFluid);
		if (!player.onGround() && player.getDeltaMovement().y < -0.08
				&& startY.getOrDefault(id, player.getY()) - player.getY() >= 3.0) {
			falling.put(id, true);
		}
		boolean landedInFluid = falling.getOrDefault(id, false) && inFluid
			&& (!wasInFluid || player.onGround() || player.getDeltaMovement().y >= 0.0);
		previouslyInFluid.put(id, inFluid);
		if (landedInFluid) {
			return true;
		}
		return falling.getOrDefault(id, false) && inFluid && player.onGround();
	}

	private static boolean inFluid(ServerPlayer player) {
		return player.isInWater()
			|| player.level().getFluidState(player.blockPosition()).is(FluidTags.WATER)
			|| player.level().getFluidState(player.getOnPos()).is(FluidTags.WATER);
	}
}
