package com.edwinc125.minuterush.task;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class TaskText {
	private TaskText() {}

	public static Component item(Item item) {
		return item.getName(new ItemStack(item)).copy().withStyle(ChatFormatting.GOLD);
	}

	public static Component block(Block block) {
		return block.getName().copy().withStyle(ChatFormatting.AQUA);
	}

	public static String itemName(Item item) {
		return item.getName(new ItemStack(item)).getString();
	}

	public static String blockName(Block block) {
		return block.getName().getString();
	}
}
