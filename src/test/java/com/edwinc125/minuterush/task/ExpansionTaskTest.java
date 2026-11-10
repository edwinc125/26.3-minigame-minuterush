package com.edwinc125.minuterush.task;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ExpansionTaskTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void decimalAnswersMatchWithoutAcceptingFractions() {
		MathTask task = new MathTask("1 / 2", new BigDecimal("0.5"));
		UUID player = UUID.randomUUID(), other = UUID.randomUUID();
		task.begin(player);
		task.accept(player, "1/2");
		assertFalse(task.completed(player));
		task.accept(other, "0.5");
		assertFalse(task.completed(other));
		for (String input : List.of("0.5", ".5", "0.50", "+0.500")) {
			task.begin(player);
			task.accept(player, input);
			assertTrue(task.completed(player), input);
		}
		for (String input : List.of("1/2", "0.6", "5e-1", "NaN", "")) {
			assertFalse(task.accepts(input), input);
		}
		assertTrue(new MathTask("1 - 4", BigDecimal.valueOf(-3)).accepts("-3"));
	}

	@Test
	void lookBoundariesAndCrouchTransitions() {
		assertFalse(LookTask.matches(-89, true));
		assertTrue(LookTask.matches(-89.01f, true));
		assertFalse(LookTask.matches(89, false));
		assertTrue(LookTask.matches(89.01f, false));
		for (int target : new int[] {3, 5, 7}) {
			CrouchProgress progress = new CrouchProgress(target, true);
			assertFalse(progress.update(true));
			for (int i = 1; i <= target; i++) {
				assertFalse(progress.update(false));
				assertEquals(i == target, progress.update(true));
			}

			assertTrue(progress.update(false));
			assertTrue(progress.update(true));
		}
	}

	@Test
	void deathRequiresTheRightCauseAfterTheTaskStartsForThatPlayer() {
		UUID player = UUID.randomUUID(), other = UUID.randomUUID();
		DeathTask task = new DeathTask("death:fall", "Fall to your death");
		EntityTaskProgress.record(player, "death:fall");
		task.begin(player);
		EntityTaskProgress.record(player, "death:lava");
		EntityTaskProgress.record(other, "death:fall");
		assertFalse(task.isComplete(player));
		assertFalse(task.isComplete(other));
		EntityTaskProgress.record(player, "death:fall");
		assertTrue(task.isComplete(player));
		task.begin(player);
		assertFalse(task.isComplete(player));
	}

	@Test
	void anyWoodCraftCountsOnlyNewMatchingCrafts() {
		UUID player = UUID.randomUUID();
		java.util.Map<Item, Integer> counts = new java.util.HashMap<>();
		counts.put(Items.OAK_BUTTON, 5);
		CraftAnyItemTask task = new CraftAnyItemTask("Craft a wooden button of any type",
			List.of(Items.OAK_BUTTON, Items.POPLAR_BUTTON, Items.CRIMSON_BUTTON));
		java.util.function.ToIntFunction<Item> count = item -> counts.getOrDefault(item, 0);
		task.begin(player, count);
		counts.put(Items.OAK_FENCE, 1);
		assertFalse(task.isComplete(player, count));
		counts.put(Items.POPLAR_BUTTON, 1);
		assertTrue(task.isComplete(player, count));
	}

	@Test
	void generatedChatTargetsHaveExpectedLengthsAndCaseRules() {
		List<TaskFactory.Selection> tasks = TaskFactory.category(null, "quickfire");
		for (String id : List.of("number", "letters", "alphanumeric")) {
			ChatTask task = (ChatTask) tasks.stream().filter(s -> s.id().equals("quickfire-type-" + id))
				.findFirst().orElseThrow().task();
			String target = task.description().substring(5, task.description().length() - " in chat".length());
			assertEquals(10, target.length());
			assertTrue(task.matches(target));
			assertTrue(task.matches(target.toUpperCase(java.util.Locale.ROOT)));
			assertFalse(task.matches(target + "x"));
			if (id.equals("number")) assertTrue(target.matches("[0-9]{10}"));
			if (id.equals("letters")) assertTrue(target.matches("[a-z]{10}"));
			if (id.equals("alphanumeric")) assertTrue(target.matches("[a-z0-9]{10}"));
			if (id.equals("alphanumeric")) {
				assertTrue(target.matches(".*[a-z].*"));
				assertTrue(target.matches(".*[0-9].*"));
			}
		}
		ChatTask any = (ChatTask) tasks.stream().filter(s -> s.id().equals("quickfire-type-any"))
			.findFirst().orElseThrow().task();
		assertFalse(any.matches("123456789"));
		assertTrue(any.matches("1234567890"));
	}

	@Test
	void generatedMathAnswersMatchEveryExpressionAcrossSeeds() {
		for (int seed = 0; seed < 100; seed++) {
			List<TaskFactory.Selection> tasks = MathCatalog.generate(new Random(seed));
			assertEquals(tasks.size(), tasks.stream().map(TaskFactory.Selection::id).distinct().count());
			assertTrue(tasks.size() >= 30);
			for (TaskFactory.Selection selection : tasks) {
				MathTask task = (MathTask) selection.task();
				String expression = task.description().substring("Type the answer in chat: ".length());
				assertEquals(0, evaluate(expression).compareTo(new BigDecimal(task.answer())), expression);
				assertTrue(task.accepts(task.answer()), expression);
				assertEquals(selection.id().startsWith("math-simple-") ? TaskDifficulty.SIMPLE : TaskDifficulty.EASY,
					selection.difficulty());
			}
		}
	}

	private static BigDecimal evaluate(String expression) {
		String text = expression.replace("(", "").replace(")", "");
		String[] parts = text.split(" ");
		List<BigDecimal> values = new java.util.ArrayList<>();
		List<String> operators = new java.util.ArrayList<>();
		values.add(new BigDecimal(parts[0]));
		for (int i = 1; i < parts.length; i += 2) {
			String operator = parts[i];
			BigDecimal value = new BigDecimal(parts[i + 1]);
			if (operator.equals("*") || operator.equals("/")) {
				int last = values.size() - 1;
				values.set(last, operator.equals("*") ? values.get(last).multiply(value) : values.get(last).divide(value));
			} else {
				operators.add(operator);
				values.add(value);
			}
		}
		BigDecimal result = values.get(0);
		for (int i = 0; i < operators.size(); i++) {
			result = operators.get(i).equals("+") ? result.add(values.get(i + 1)) : result.subtract(values.get(i + 1));
		}
		return result;
	}

	@Test
	void catalogsIncludeEveryWoodVariantAndDeathCause() {
		List<TaskFactory.Selection> crafting = TaskFactory.category(null, "crafting");
		Set<String> ids = crafting.stream().map(TaskFactory.Selection::id).collect(Collectors.toSet());
		for (String kind : List.of("fence", "fence-gate", "sign", "pressure-plate", "button", "slab",
				"stairs", "door", "trapdoor", "shelf", "hanging-sign")) {
			assertTrue(ids.contains("craft-wooden-" + kind), kind);
			for (String wood : List.of("oak", "spruce", "birch", "jungle", "acacia", "dark-oak",
					"mangrove", "cherry", "pale-oak", "poplar", "bamboo", "crimson", "warped")) {
				assertTrue(ids.contains("craft-wood-" + wood + ":" + kind), wood + " " + kind);
			}
		}
		assertEquals("Craft an oak wood button", crafting.stream()
			.filter(s -> s.id().equals("craft-wood-oak:button")).findFirst().orElseThrow().task().description());
		assertEquals("Craft a wooden button of any type", crafting.stream()
			.filter(s -> s.id().equals("craft-wooden-button")).findFirst().orElseThrow().task().description());
		assertEquals(15, TaskFactory.category(null, "death").size());
		assertNotNull(TaskRequirements.of("craft-wood-poplar:button"));
		assertNotNull(TaskRequirements.of("craft-wood-pale-oak:shelf"));
		assertNotNull(TaskRequirements.of("basic:shelf-mushroom:collect"));
		assertNotNull(TaskRequirements.of("basic:stripped-poplar-wood:mine"));
		assertFalse(TaskRequirements.of("craft-wood-crimson:button").isMet(biome -> true, true));
	}
}
