package com.edwinc125.minuterush.command;

import com.edwinc125.minuterush.game.MinuteRushGame;
import java.util.function.Supplier;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

public final class MinuteRushCommands {
	private MinuteRushCommands() {}

	public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher,
			Supplier<MinuteRushGame> gameSupplier) {
		dispatcher.register(Commands.literal("minute-rush")
			.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
			.then(Commands.literal("start").executes(context -> execute(context.getSource(), gameSupplier, "start")))
			.then(Commands.literal("solo-start")
				.executes(context -> soloStart(context.getSource(), gameSupplier)))
			.then(Commands.literal("solo-exit")
				.executes(context -> execute(context.getSource(), gameSupplier, "solo-exit")))
			.then(Commands.literal("skip")
				.executes(context -> execute(context.getSource(), gameSupplier, "skip")))
			.then(Commands.literal("test")
				.then(Commands.literal("skip")
					.executes(context -> testSkip(context.getSource(), gameSupplier)))
				.then(Commands.argument("category", StringArgumentType.word())
					.suggests((context, builder) -> SharedSuggestionProvider.suggest(
						java.util.List.of("movement", "crafting", "combat", "basic", "interactive"), builder))
					.executes(context -> testStart(context.getSource(), gameSupplier,
						StringArgumentType.getString(context, "category")))))
			.then(Commands.literal("reset").executes(context -> execute(context.getSource(), gameSupplier, "reset")))
			.then(Commands.literal("config")
				.then(Commands.literal("score")
					.then(Commands.argument("value", IntegerArgumentType.integer(1))
						.executes(context -> configure(context.getSource(), gameSupplier, "score",
							IntegerArgumentType.getInteger(context, "value")))))
				.then(Commands.literal("duration")
					.then(Commands.argument("seconds", IntegerArgumentType.integer(1))
						.executes(context -> configure(context.getSource(), gameSupplier, "duration",
							IntegerArgumentType.getInteger(context, "seconds"))))))
			.then(Commands.literal("status").requires(source -> true)
				.executes(context -> status(context.getSource(), gameSupplier))));
	}

	private static int configure(CommandSourceStack source, Supplier<MinuteRushGame> supplier, String setting, int value) {
		MinuteRushGame game = supplier.get();
		if (game == null) {
			source.sendFailure(Component.literal("The server is not ready."));
			return 0;
		}
		if (setting.equals("score")) {
			game.setTargetScore(value);
		} else {
			game.setRoundDurationSeconds(value);
		}
		source.sendSuccess(() -> Component.literal("Updated " + setting + " to " + value + "."), true);
		return 1;
	}

	private static int execute(CommandSourceStack source, Supplier<MinuteRushGame> supplier, String action) {
		MinuteRushGame game = supplier.get();
		if (game == null) {
			source.sendFailure(Component.literal("The server is not ready."));
			return 0;
		}
		String message = switch (action) {
			case "start" -> game.start();
			case "solo-exit" -> game.reset("Singleplayer test mode stopped.");
			case "skip" -> source.getPlayer() != null && game.isCategoryTest(source.getPlayer())
				? game.skipTestTask(source.getPlayer()) : game.skipRound();
			case "reset" -> game.reset("Match reset.");
			default -> "Unknown command action.";
		};
		source.sendSuccess(() -> Component.literal(message), true);
		return 1;
	}

	private static int testStart(CommandSourceStack source, Supplier<MinuteRushGame> supplier, String category) {
		MinuteRushGame game = supplier.get();
		if (game == null || source.getPlayer() == null) {
			source.sendFailure(Component.literal("Run this command as an operator player in a world."));
			return 0;
		}
		String message = game.startCategoryTest(source.getPlayer(), category);
		if (message.startsWith("Unknown") || message.startsWith("No tasks")) {
			source.sendFailure(Component.literal(message));
			return 0;
		}
		source.sendSuccess(() -> Component.literal(message), true);
		return 1;
	}

	private static int testSkip(CommandSourceStack source, Supplier<MinuteRushGame> supplier) {
		MinuteRushGame game = supplier.get();
		if (game == null || source.getPlayer() == null) {
			source.sendFailure(Component.literal("Run this command as an operator player in a world."));
			return 0;
		}
		String message = game.skipTestTask(source.getPlayer());
		if (message.startsWith("No category")) {
			source.sendFailure(Component.literal(message));
			return 0;
		}
		source.sendSuccess(() -> Component.literal(message), true);
		return 1;
	}

	private static int soloStart(CommandSourceStack source, Supplier<MinuteRushGame> supplier) {
		MinuteRushGame game = supplier.get();
		if (game == null || source.getPlayer() == null) {
			source.sendFailure(Component.literal("Run this command as a player in a world."));
			return 0;
		}
		source.sendSuccess(() -> Component.literal(game.startSolo(source.getPlayer())), true);
		return 1;
	}

	private static int status(CommandSourceStack source, Supplier<MinuteRushGame> supplier) {
		MinuteRushGame game = supplier.get();
		if (game == null) {
			source.sendFailure(Component.literal("The server is not ready."));
			return 0;
		}
		source.sendSuccess(() -> Component.literal(game.status()), false);
		return 1;
	}
}
