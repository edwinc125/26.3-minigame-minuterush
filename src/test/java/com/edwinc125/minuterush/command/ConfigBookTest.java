package com.edwinc125.minuterush.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.edwinc125.minuterush.config.MinuteRushConfig;
import com.edwinc125.minuterush.task.MatchDifficulty;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ConfigBookTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void offersEveryChoiceFromTheProposal() {
		List<String> commands = runCommands(ConfigBook.pages(MinuteRushConfig.DEFAULT, false));
		for (String args : List.of("difficulty easy", "difficulty medium", "difficulty hard", "pvp off",
				"score 6", "score 8", "score 12", "score 15", "duration 30", "duration 45", "duration 90")) {
			assertTrue(commands.contains(ConfigBook.command(args)), args + " missing from " + commands);
		}
	}

	@Test
	void currentValuesAreMarkedAndNotClickable() {
		// Defaults: progressive, PvP on, 10 points, 60 seconds.
		List<String> commands = runCommands(ConfigBook.pages(MinuteRushConfig.DEFAULT, false));
		for (String args : List.of("difficulty progressive", "pvp on", "score 10", "duration 60")) {
			assertFalse(commands.contains(ConfigBook.command(args)), args + " is already selected");
		}
		String text = text(ConfigBook.pages(MinuteRushConfig.DEFAULT, false));
		assertTrue(text.contains("● Progressive"), text);
		assertTrue(text.contains("● 10"), text);
	}

	@Test
	void customValuesAreShownAndCanBeTyped() {
		MinuteRushConfig config = MinuteRushConfig.DEFAULT.withTargetScore(7).withRoundDurationSeconds(75);
		List<Component> pages = ConfigBook.pages(config, false);
		String text = text(pages);
		assertTrue(text.contains("● 7 (custom)"), text);
		assertTrue(text.contains("● 75s (custom)"), text);
		assertTrue(clicks(pages).stream().anyMatch(click -> click instanceof ClickEvent.SuggestCommand suggest
			&& suggest.command().equals("/minute-rush config score ")));
	}

	@Test
	void lockedBookHasNoSettingButtons() {
		List<Component> pages = ConfigBook.pages(MinuteRushConfig.DEFAULT.withDifficulty(MatchDifficulty.HARD), true);
		assertEquals(List.of(), runCommands(pages));
		assertFalse(clicks(pages).stream().anyMatch(click -> click instanceof ClickEvent.SuggestCommand));
		assertTrue(text(pages).contains("Locked during a match."));
	}

	@Test
	void everyButtonRunsARealCommandAnyoneCanUse() {
		CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
		MinuteRushCommands.register(dispatcher, () -> null);
		MinuteRushConfig config = MinuteRushConfig.DEFAULT.withPvp(false).withDifficulty(MatchDifficulty.EASY);
		List<String> commands = new ArrayList<>(runCommands(ConfigBook.pages(config, false)));
		commands.add("/minute-rush config");
		commands.add("/minute-rush status");
		for (String command : commands) {
			// A null source only passes nodes without a permission requirement, i.e. ones non-operators can use.
			ParseResults<CommandSourceStack> parse = dispatcher.parse(command.substring(1), null);
			assertFalse(parse.getReader().canRead(), command + " did not parse fully");
			assertNotNull(parse.getContext().getCommand(), command + " has no command");
		}
	}

	private static List<String> runCommands(List<Component> pages) {
		return clicks(pages).stream()
			.filter(click -> click instanceof ClickEvent.RunCommand)
			.map(click -> ((ClickEvent.RunCommand) click).command())
			.toList();
	}

	private static List<ClickEvent> clicks(List<Component> pages) {
		List<ClickEvent> clicks = new ArrayList<>();
		for (Component page : pages) {
			collect(page, clicks);
		}
		return clicks;
	}

	private static void collect(Component component, List<ClickEvent> clicks) {
		if (component.getStyle().getClickEvent() != null) {
			clicks.add(component.getStyle().getClickEvent());
		}
		for (Component sibling : component.getSiblings()) {
			collect(sibling, clicks);
		}
	}

	private static String text(List<Component> pages) {
		StringBuilder text = new StringBuilder();
		for (Component page : pages) {
			text.append(page.getString()).append('\n');
		}
		return text.toString();
	}
}
