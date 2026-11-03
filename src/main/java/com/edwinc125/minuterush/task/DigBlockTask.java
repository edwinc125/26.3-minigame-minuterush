package com.edwinc125.minuterush.task;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.block.Block;

public final class DigBlockTask implements MinuteRushTask {
	private final Block block;
	private final BlockPos target;
	private final java.util.Map<java.util.UUID, Integer> baselines = new java.util.HashMap<>();

	public DigBlockTask(Block block, BlockPos target) {
		this.block = block;
		this.target = target.immutable();
	}

	@Override
	public String description() {
		return "Mine a " + TaskText.blockName(block).toLowerCase(java.util.Locale.ROOT);
	}

	@Override
	public void begin(ServerPlayer player) {
		baselines.put(player.getUUID(), player.getStats().getValue(Stats.BLOCK_MINED, block));
		player.sendSystemMessage(Component.literal(description()).withStyle(net.minecraft.ChatFormatting.AQUA));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		int baseline = baselines.getOrDefault(player.getUUID(), 0);
		return player.getStats().getValue(Stats.BLOCK_MINED, block) > baseline
			|| player.level().getBlockState(target).isAir();
	}
}
