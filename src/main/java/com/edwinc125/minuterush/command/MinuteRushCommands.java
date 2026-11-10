package com.edwinc125.minuterush.command;

import com.edwinc125.minuterush.game.MinuteRushGame;
import com.edwinc125.minuterush.task.MatchDifficulty;
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
		// Anyone can use "config" (the settings book) and "status"; everything else needs an operator.
		dispatcher.register(Commands.literal("minute-rush")
			.then(Commands.literal("start").requires(MinuteRushCommands::isOperator)
				.executes(context -> execute(context.getSource(), gameSupplier, "start")))
			.then(Commands.literal("solo-start").requires(MinuteRushCommands::isOperator)
				.executes(context -> soloStart(context.getSource(), gameSupplier)))
			.then(Commands.literal("solo-exit").requires(MinuteRushCommands::isOperator)
				.executes(context -> execute(context.getSource(), gameSupplier, "solo-exit")))
			.then(Commands.literal("skip").requires(MinuteRushCommands::isOperator)
				.executes(context -> execute(context.getSource(), gameSupplier, "skip")))
			.then(Commands.literal("test").requires(MinuteRushCommands::isOperator)
				.then(Commands.literal("skip")
					.executes(context -> testSkip(context.getSource(), gameSupplier)))
				.then(Commands.argument("category", StringArgumentType.word())
					.suggests((context, builder) -> SharedSuggestionProvider.suggest(
						java.util.List.of("movement", "crafting", "combat", "basic", "interactive",
							"death", "quickfire", "maths"), builder))
					.executes(context -> testStart(context.getSource(), gameSupplier,
						StringArgumentType.getString(context, "category")))))
			.then(Commands.literal("reset").requires(MinuteRushCommands::isOperator)
				.executes(context -> execute(context.getSource(), gameSupplier, "reset")))
			.then(Commands.literal("config")
				.executes(context -> openBook(context.getSource(), gameSupplier))
				.then(Commands.literal("score")
					.then(withBook(Commands.argument("value", IntegerArgumentType.integer(1)), (context, book) -> {
						int value = IntegerArgumentType.getInteger(context, "value");
						return change(context.getSource(), gameSupplier, book, "points to win", String.valueOf(value),
							game -> game.setTargetScore(value));
					})))
				.then(Commands.literal("duration")
					.then(withBook(Commands.argument("seconds", IntegerArgumentType.integer(1)), (context, book) -> {
						int seconds = IntegerArgumentType.getInteger(context, "seconds");
						return change(context.getSource(), gameSupplier, book, "time per round", seconds + "s",
							game -> game.setRoundDurationSeconds(seconds));
					})))
				.then(Commands.literal("difficulty")
					.then(withBook(Commands.argument("mode", StringArgumentType.word())
						.suggests((context, builder) -> SharedSuggestionProvider.suggest(
							java.util.Arrays.stream(MatchDifficulty.values()).map(MatchDifficulty::id), builder)),
						(context, book) -> {
							String mode = StringArgumentType.getString(context, "mode");
							MatchDifficulty difficulty = MatchDifficulty.parse(mode);
							if (difficulty == null) {
								context.getSource().sendFailure(Component.literal("Unknown difficulty \"" + mode
									+ "\". Use easy, medium, hard or progressive."));
								return 0;
							}
							return change(context.getSource(), gameSupplier, book, "difficulty", difficulty.id(),
								game -> game.setDifficulty(difficulty));
						})))
				.then(Commands.literal("pvp")
					.then(withBook(Commands.literal("on"), (context, book) -> change(context.getSource(),
						gameSupplier, book, "PvP", "on", game -> game.setPvp(true))))
					.then(withBook(Commands.literal("off"), (context, book) -> change(context.getSource(),
						gameSupplier, book, "PvP", "off", game -> game.setPvp(false))))))
			.then(Commands.literal("status")
				.executes(context -> status(context.getSource(), gameSupplier))));
		dispatcher.register(Commands.literal("voteskip")
			.executes(context -> voteSkip(context.getSource(), gameSupplier)));
	}

	private static boolean isOperator(CommandSourceStack source) {
		return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	private interface SettingChange {
		int run(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, boolean reopenBook)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException;
	}

	/** Makes the node run the change, and adds a trailing "book" (used by the book's buttons) that reopens the book. */
	private static <T extends com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, T>> T withBook(
			T node, SettingChange change) {
		return node.executes(context -> change.run(context, false))
			.then(Commands.literal("book").executes(context -> change.run(context, true)));
	}

	private static int openBook(CommandSourceStack source, Supplier<MinuteRushGame> supplier) {
		MinuteRushGame game = supplier.get();
		if (game == null || source.getPlayer() == null) {
			source.sendFailure(Component.literal("Run this command as a player in a world."));
			return 0;
		}
		ConfigBook.open(source.getPlayer(), game.config(), isLocked(game));
		return 1;
	}

	/** Nobody, operators included, can change settings while a match is running (issue #39). */
	private static boolean isLocked(MinuteRushGame game) {
		return game.isMatchInProgress();
	}

	private static int change(CommandSourceStack source, Supplier<MinuteRushGame> supplier, boolean reopenBook,
			String setting, String value, java.util.function.Consumer<MinuteRushGame> apply) {
		MinuteRushGame game = supplier.get();
		if (game == null) {
			source.sendFailure(Component.literal("The server is not ready."));
			return 0;
		}
		if (isLocked(game)) {
			source.sendFailure(Component.literal("Settings can't be changed during a match."));
			return 0;
		}
		apply.accept(game);
		// Tell everyone, since any player can change settings.
		source.getServer().getPlayerList().broadcastSystemMessage(
			Component.literal(source.getTextName() + " set " + setting + " to " + value + "."), false);
		if (reopenBook && source.getPlayer() != null) {
			ConfigBook.open(source.getPlayer(), game.config(), isLocked(game));
		}
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

	private static int voteSkip(CommandSourceStack source, Supplier<MinuteRushGame> supplier) {
		MinuteRushGame game = supplier.get();
		if (game == null || source.getPlayer() == null) {
			source.sendFailure(Component.literal("Run this command as a player in a world."));
			return 0;
		}
		String message = game.voteSkip(source.getPlayer());
		if (!message.equals(MinuteRushGame.VOTE_RECORDED)) {
			source.sendFailure(Component.literal(message));
			return 0;
		}
		source.sendSuccess(() -> Component.literal(message), false);
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
