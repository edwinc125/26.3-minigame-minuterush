package com.edwinc125.minuterush.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TravelTask implements MinuteRushTask {
	private final double distance;
	private final String direction;
	private final double directionX;
	private final double directionZ;
	private final Map<UUID, double[]> starts = new HashMap<>();

	public TravelTask(double distance) {
		this("any direction", 0.0, 0.0, distance);
	}

	public TravelTask(String direction, double directionX, double directionZ, double distance) {
		this.direction = direction;
		this.directionX = directionX;
		this.directionZ = directionZ;
		this.distance = distance;
	}

	@Override
	public String description() {
		return "Travel " + ("any direction".equals(direction) ? "" : direction + " ")
			+ (int) distance + " blocks";
	}

	@Override
	public void begin(ServerPlayer player) {
		starts.put(player.getUUID(), new double[] {player.getX(), player.getZ()});
		player.sendSystemMessage(Component.literal(description()).withStyle(net.minecraft.ChatFormatting.BLUE));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		double[] start = starts.get(player.getUUID());
		if (start == null) {
			begin(player);
			start = starts.get(player.getUUID());
		}
		double deltaX = player.getX() - start[0];
		double deltaZ = player.getZ() - start[1];
		if ("any direction".equals(direction)) {
			return Math.hypot(deltaX, deltaZ) >= distance;
		}
		return deltaX * directionX + deltaZ * directionZ >= distance;
	}
}
