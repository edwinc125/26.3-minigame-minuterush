package com.edwinc125.minuterush.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class InteractionTask implements MinuteRushTask {
	public enum Kind { PLAYER, MOB, BLOCK }

	private final Kind kind;
	private final String target;
	private final int required;
	private final Map<UUID, Integer> progress = new HashMap<>();

	public InteractionTask(Kind kind, String target, int required) {
		this.kind = kind;
		this.target = target;
		this.required = required;
	}

	@Override
	public String description() {
		if (kind == Kind.BLOCK) {
			return switch (target) {
				case "chest" -> "Open a chest";
				case "barrel" -> "Open a barrel";
				case "furnace" -> "Open a furnace";
				case "crafting_table" -> "Open a crafting table";
				case "button" -> "Press a button";
				case "lever" -> "Pull a lever";
				case "pressure_plate" -> "Step on a pressure plate";
				default -> "Interact with " + ("any-block".equals(target) ? "an interactable block" : "a " + target);
			};
		}
		return switch (kind) {
			case PLAYER -> "Use or hit another player";
			case MOB -> "Use or hit " + ("any-mob".equals(target) ? "any mob" : "a " + target);
			case BLOCK -> "Interact with " + target;
		};
	}

	@Override
	public void begin(ServerPlayer player) {
		progress.put(player.getUUID(), 0);
		player.sendSystemMessage(Component.literal(description()).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
	}

	public void record(ServerPlayer player, String targetId) {
		if (accepts(targetId)) {
			progress.merge(player.getUUID(), 1, Integer::sum);
		}
	}

	public boolean accepts(String targetId) {
		return (kind == Kind.PLAYER && "player".equals(targetId))
			|| (kind == Kind.MOB && ("any-mob".equals(target) || target.equals(targetId)))
			|| (kind == Kind.BLOCK && acceptsBlock(targetId));
	}

	private boolean acceptsBlock(String targetId) {
		if ("any-block".equals(target)) {
			return true;
		}
		return switch (target) {
			case "chest" -> "chest".equals(targetId) || "trapped_chest".equals(targetId);
			case "button" -> targetId.endsWith("_button");
			case "pressure_plate" -> targetId.endsWith("_pressure_plate");
			default -> target.equals(targetId);
		};
	}

	@Override
	public boolean isComplete(ServerPlayer player) {
		return progress.getOrDefault(player.getUUID(), 0) >= required;
	}
}
