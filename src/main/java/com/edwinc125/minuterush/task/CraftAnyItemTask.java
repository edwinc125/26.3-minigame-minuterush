package com.edwinc125.minuterush.task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;

public final class CraftAnyItemTask implements MinuteRushTask {
	private final String description;
	private final List<Item> items;
	private final Map<UUID, Integer> baselines = new HashMap<>();

	public CraftAnyItemTask(String description, List<Item> items) {
		this.description = description;
		this.items = List.copyOf(items);
	}

	@Override
	public String description() {
		return description;
	}

	@Override
	public void begin(ServerPlayer player) {
		begin(player.getUUID(), item -> player.getStats().getValue(Stats.ITEM_CRAFTED, item));
		player.sendSystemMessage(Component.literal(description).withStyle(net.minecraft.ChatFormatting.YELLOW));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		return isComplete(player.getUUID(), item -> player.getStats().getValue(Stats.ITEM_CRAFTED, item));
	}

	void begin(UUID player, ToIntFunction<Item> count) {
		baselines.put(player, total(count));
	}

	boolean isComplete(UUID player, ToIntFunction<Item> count) {
		return baselines.containsKey(player) && total(count) > baselines.get(player);
	}

	private int total(ToIntFunction<Item> count) {
		int total = 0;
		for (Item item : items) total += count.applyAsInt(item);
		return total;
	}
}
