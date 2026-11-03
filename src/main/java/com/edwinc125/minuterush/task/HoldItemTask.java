package com.edwinc125.minuterush.task;

import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class HoldItemTask implements MinuteRushTask {
	private final String prompt;
	private final Predicate<ItemStack> item;

	public HoldItemTask(String prompt, Predicate<ItemStack> item) {
		this.prompt = prompt;
		this.item = item;
	}

	@Override
	public String description() {
		return prompt;
	}

	@Override
	public void begin(ServerPlayer player) {
		player.sendSystemMessage(Component.literal(prompt).withStyle(net.minecraft.ChatFormatting.GREEN));
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		return item.test(player.getMainHandItem()) || item.test(player.getOffhandItem());
	}
}
