package com.edwinc125.minuterush.game;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class LobbyItems {
	public enum Action {
		READY,
		UNREADY,
		SPECTATE,
		JOIN,
		NONE
	}

	private static final String READY = "READY TO PLAY";
	private static final String UNREADY = "UN-READY";
	private static final String SPECTATE = "SPECTATE THIS GAME";
	private static final String JOIN = "JOIN THIS GAME";

	private LobbyItems() {}

	public static void apply(ServerPlayer player, boolean ready, boolean spectator) {
		removeControls(player);
		if (spectator) {
			player.getInventory().setItem(8, item(Items.CONCRETE.lightBlue(), JOIN, ChatFormatting.AQUA));
		} else if (ready) {
			player.getInventory().setItem(8, item(Items.CONCRETE.red(), UNREADY, ChatFormatting.RED));
		} else {
			player.getInventory().setItem(7, item(Items.CONCRETE.lime(), READY, ChatFormatting.GREEN));
			player.getInventory().setItem(8, item(Items.CONCRETE.yellow(), SPECTATE, ChatFormatting.YELLOW));
		}
		player.inventoryMenu.broadcastChanges();
	}

	public static Action action(ItemStack stack) {
		if (stack.isEmpty() || !stack.has(DataComponents.CUSTOM_NAME)) {
			return Action.NONE;
		}
		String name = stack.get(DataComponents.CUSTOM_NAME).getString();
		if (name.startsWith(READY)) return Action.READY;
		if (name.startsWith(UNREADY)) return Action.UNREADY;
		if (name.startsWith(SPECTATE)) return Action.SPECTATE;
		if (name.startsWith(JOIN)) return Action.JOIN;
		return Action.NONE;
	}

	public static boolean isControl(ItemStack stack) {
		return action(stack) != Action.NONE;
	}

	private static ItemStack item(net.minecraft.world.item.Item item, String name, ChatFormatting color) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.CUSTOM_NAME,
			Component.literal(name).withStyle(ChatFormatting.BOLD, color)
				.append(Component.literal(" (Right click)").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)));
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		return stack;
	}

	private static void removeControls(ServerPlayer player) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			if (isControl(player.getInventory().getItem(slot))) {
				player.getInventory().setItem(slot, ItemStack.EMPTY);
			}
		}
	}
}
