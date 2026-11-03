package com.edwinc125.minuterush.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;

public final class CraftItemTask implements MinuteRushTask {
	private final Item item;
	private final Map<UUID, Integer> baselines = new HashMap<>();

	public CraftItemTask(Item item) {
		this.item = item;
	}

	@Override
	public String description() {
		return "Craft a " + TaskText.itemName(item).toLowerCase(java.util.Locale.ROOT);
	}

	@Override
	public void begin(ServerPlayer player) {
		baselines.put(player.getUUID(), player.getStats().getValue(Stats.ITEM_CRAFTED, item));
		player.sendSystemMessage(Component.literal(description()).withStyle(net.minecraft.ChatFormatting.YELLOW));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		return player.getStats().getValue(Stats.ITEM_CRAFTED, item)
			> baselines.getOrDefault(player.getUUID(), 0);
	}
}
