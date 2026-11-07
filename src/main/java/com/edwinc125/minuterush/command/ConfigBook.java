package com.edwinc125.minuterush.command;

import com.edwinc125.minuterush.config.MinuteRushConfig;
import com.edwinc125.minuterush.task.MatchDifficulty;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

/**
 * The settings book opened by {@code /minute-rush config} (discussion #27). Each option runs the matching
 * {@code /minute-rush config <setting> <value> book} command, which applies it and reopens the book.
 */
public final class ConfigBook {
	static final int[] POINT_OPTIONS = {6, 8, 10, 12, 15};
	static final int[] SECOND_OPTIONS = {30, 45, 60, 90};
	static final String COMMAND = "/minute-rush config";

	private static final int DIFFICULTY_PAGE = 2;
	private static final int PVP_PAGE = 3;
	private static final int POINTS_PAGE = 4;
	private static final int TIME_PAGE = 5;

	private ConfigBook() {}

	/**
	 * Opens the book without touching the player's inventory: the client is told it holds the book, opens it, and
	 * is then sent its real held item back. Nothing changes on the server.
	 */
	public static void open(ServerPlayer player, MinuteRushConfig config, boolean locked) {
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		List<Filterable<Component>> pages = pages(config, locked).stream().map(Filterable::passThrough).toList();
		book.set(DataComponents.WRITTEN_BOOK_CONTENT,
			new WrittenBookContent(Filterable.passThrough("Minute Rush"), "Minute Rush", 0, pages, true));
		int slot = player.getInventory().getSelectedSlot();
		player.connection.send(new ClientboundSetPlayerInventoryPacket(slot, book));
		player.connection.send(new ClientboundOpenBookPacket(InteractionHand.MAIN_HAND));
		player.connection.send(new ClientboundSetPlayerInventoryPacket(slot, player.getInventory().getSelectedItem().copy()));
	}

	/** @param locked the viewer cannot change settings right now (a match is running and they are not an operator) */
	static List<Component> pages(MinuteRushConfig config, boolean locked) {
		return List.of(overview(config, locked), difficultyPage(config, locked), pvpPage(config, locked),
			pointsPage(config, locked), timePage(config, locked));
	}

	private static Component overview(MinuteRushConfig config, boolean locked) {
		MutableComponent page = Component.empty()
			.append(Component.literal("Minute Rush\n").withStyle(ChatFormatting.BOLD))
			.append(Component.literal("Match settings\n\n").withStyle(ChatFormatting.DARK_GRAY))
			.append(summary("Difficulty", capitalize(config.difficulty().id()), DIFFICULTY_PAGE))
			.append(summary("PvP", config.pvp() ? "On" : "Off", PVP_PAGE))
			.append(summary("Points to win", String.valueOf(config.targetScore()), POINTS_PAGE))
			.append(summary("Time per round", config.roundDurationSeconds() + "s", TIME_PAGE));
		if (locked) {
			page.append(Component.literal("\nLocked during a match.").withStyle(ChatFormatting.DARK_RED));
		} else {
			page.append(Component.literal("\nClick a setting to change it.").withStyle(ChatFormatting.DARK_GRAY));
		}
		return page;
	}

	private static Component summary(String label, String value, int page) {
		return Component.literal(label + ":\n")
			.append(Component.literal(" " + value + " ›\n").withStyle(style -> style
				.withColor(ChatFormatting.DARK_BLUE)
				.withClickEvent(new ClickEvent.ChangePage(page))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Change " + label.toLowerCase())))));
	}

	private static Component difficultyPage(MinuteRushConfig config, boolean locked) {
		MutableComponent page = heading("Difficulty");
		for (MatchDifficulty difficulty : MatchDifficulty.values()) {
			page.append(option(capitalize(difficulty.id()), difficulty == config.difficulty(), locked,
				"difficulty " + difficulty.id(), describe(difficulty)));
		}
		return page.append(footer());
	}

	private static Component pvpPage(MinuteRushConfig config, boolean locked) {
		return heading("PvP")
			.append(option("On", config.pvp(), locked, "pvp on", "Players can hurt each other all match."))
			.append(option("Off", !config.pvp(), locked, "pvp off",
				"Players can only hurt each other during tasks that target a player."))
			.append(footer());
	}

	private static Component pointsPage(MinuteRushConfig config, boolean locked) {
		MutableComponent page = heading("Points to win");
		boolean custom = true;
		for (int points : POINT_OPTIONS) {
			custom &= points != config.targetScore();
			page.append(option(String.valueOf(points), points == config.targetScore(), locked,
				"score " + points, "First to " + points + " points wins."));
		}
		if (custom) {
			page.append(option(config.targetScore() + " (custom)", true, locked, null, null));
		}
		page.append(customOption(locked, "score ", "Type any number of points in chat."));
		return page.append(footer());
	}

	private static Component timePage(MinuteRushConfig config, boolean locked) {
		MutableComponent page = heading("Time per round");
		boolean custom = true;
		for (int seconds : SECOND_OPTIONS) {
			custom &= seconds != config.roundDurationSeconds();
			page.append(option(seconds + "s", seconds == config.roundDurationSeconds(), locked,
				"duration " + seconds, "Each round lasts " + seconds + " seconds."));
		}
		if (custom) {
			page.append(option(config.roundDurationSeconds() + "s (custom)", true, locked, null, null));
		}
		return page.append(footer());
	}

	private static MutableComponent heading(String title) {
		return Component.empty().append(Component.literal(title + "\n\n").withStyle(ChatFormatting.BOLD));
	}

	/**
	 * One choice. The current value is marked and not clickable; others run their command unless locked.
	 *
	 * @param args command arguments after "/minute-rush config", or null for a value with no button
	 */
	private static Component option(String label, boolean selected, boolean locked, String args, String hover) {
		if (selected) {
			return Component.literal("● " + label + "\n").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD);
		}
		if (locked) {
			return Component.literal("○ " + label + "\n").withStyle(style -> style
				.withColor(ChatFormatting.GRAY)
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Settings are locked during a match."))));
		}
		return Component.literal("○ " + label + "\n").withStyle(style -> style
			.withColor(ChatFormatting.DARK_BLUE)
			.withClickEvent(new ClickEvent.RunCommand(command(args)))
			.withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
	}

	/** A "Custom..." entry that fills in the chat box, since a book cannot take typed input. */
	private static Component customOption(boolean locked, String args, String hover) {
		if (locked) {
			return Component.literal("○ Custom...\n").withStyle(ChatFormatting.GRAY);
		}
		return Component.literal("○ Custom...\n").withStyle(style -> style
			.withColor(ChatFormatting.DARK_BLUE)
			.withClickEvent(new ClickEvent.SuggestCommand(COMMAND + " " + args))
			.withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
	}

	private static Component footer() {
		return Component.literal("\n‹ Back").withStyle(style -> style
			.withColor(ChatFormatting.DARK_GRAY)
			.withClickEvent(new ClickEvent.ChangePage(1)));
	}

	/** The command an option runs; the trailing "book" reopens the book with the new value selected. */
	static String command(String args) {
		return COMMAND + " " + args + " book";
	}

	private static String describe(MatchDifficulty difficulty) {
		if (difficulty.mix() == null) {
			return "Starts easy, then medium, then hard.";
		}
		return Math.round(difficulty.mix().simpleOrEasy() * 100) + "% simple/easy, "
			+ Math.round(difficulty.mix().medium() * 100) + "% medium, "
			+ Math.round(difficulty.mix().hard() * 100) + "% hard tasks.";
	}

	private static String capitalize(String id) {
		return Character.toUpperCase(id.charAt(0)) + id.substring(1);
	}
}
