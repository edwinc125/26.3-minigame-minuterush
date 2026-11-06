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
	private final String verb;
	private final Map<UUID, Integer> baselines = new HashMap<>();

	public CraftItemTask(Item item) {
		this(item, "Craft");
	}

	/** {@code verb} is shown to players, e.g. "Smelt" for furnace output (which also counts as crafted). */
	public CraftItemTask(Item item, String verb) {
		this.item = item;
		this.verb = verb;
	}

	@Override
	public String description() {
		return verb + " a " + TaskText.itemName(item).toLowerCase(java.util.Locale.ROOT);
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
