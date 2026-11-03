package com.edwinc125.minuterush.task;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.stats.Stats;

public final class CollectItemTask implements MinuteRushTask {
	private final Item item;
	private final int count;
	private final java.util.Map<java.util.UUID, Integer> baselines = new java.util.HashMap<>();
	private final java.util.Map<java.util.UUID, Integer> pickupBaselines = new java.util.HashMap<>();

	public CollectItemTask(Item item, int count) {
		this.item = item;
		this.count = count;
	}

	@Override
	public String description() {
		return "Collect " + count + " " + TaskText.itemName(item).toLowerCase(java.util.Locale.ROOT);
	}

	@Override
	public void begin(ServerPlayer player) {
		player.sendSystemMessage(Component.literal(description()).withStyle(net.minecraft.ChatFormatting.GOLD));
		baselines.put(player.getUUID(), player.getInventory().countItem(item));
		pickupBaselines.put(player.getUUID(), player.getStats().getValue(Stats.ITEM_PICKED_UP, item));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		int inventoryDelta = player.getInventory().countItem(item) - baselines.getOrDefault(player.getUUID(), 0);
		int pickupDelta = player.getStats().getValue(Stats.ITEM_PICKED_UP, item)
			- pickupBaselines.getOrDefault(player.getUUID(), 0);
		return inventoryDelta >= count || pickupDelta >= count;
	}
}
