package com.edwinc125.minuterush.task;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.block.Block;

public final class PlaceBlockTask implements MinuteRushTask {
	private final Block block;
	private final java.util.Map<java.util.UUID, Integer> baselines = new java.util.HashMap<>();

	public PlaceBlockTask(Block block) {
		this.block = block;
	}

	@Override
	public String description() {
		return "Place a " + TaskText.blockName(block).toLowerCase(java.util.Locale.ROOT) + " anywhere";
	}

	@Override
	public void begin(ServerPlayer player) {
		player.sendSystemMessage(Component.literal(description()).withStyle(net.minecraft.ChatFormatting.AQUA));
		baselines.put(player.getUUID(), player.getStats().getValue(Stats.ITEM_USED, block.asItem()));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		int baseline = baselines.getOrDefault(player.getUUID(), 0);
		return player.getStats().getValue(Stats.ITEM_USED, block.asItem()) > baseline;
	}
}
