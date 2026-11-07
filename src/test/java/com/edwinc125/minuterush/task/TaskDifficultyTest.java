package com.edwinc125.minuterush.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Random;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TaskDifficultyTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void everyTaskInThePoolIsClassified() {
		for (TaskFactory.Selection selection : TaskFactory.all(null)) {
			if (selection.id().startsWith("stone:")) {
				continue; // stone tasks take their difficulty from the catalog rarity
			}
			assertNotNull(TaskDifficulty.classify(selection.id()), selection.id() + " has no difficulty");
		}
	}

	@Test
	void examplesFromTheProposal() {
		assertEquals(TaskDifficulty.SIMPLE, TaskDifficulty.of("walk-25"));
		assertEquals(TaskDifficulty.EASY, TaskDifficulty.of("basic:oak-log:mine"));
		assertEquals(TaskDifficulty.EASY, TaskDifficulty.of("craft-copper-pickaxe"));
		assertEquals(TaskDifficulty.MEDIUM, TaskDifficulty.of("craft-iron-pickaxe"));
		assertEquals(TaskDifficulty.MEDIUM, TaskDifficulty.of("basic:apple:collect"));
		assertEquals(TaskDifficulty.MEDIUM, TaskDifficulty.of("basic:redstone-dust:hold"));
		assertEquals(TaskDifficulty.HARD, TaskDifficulty.of("basic:diamond:collect"));
	}

	@Test
	void warmUpOnlyDrawsSimpleAndEasyTasks() {
		Random random = new Random(7);
		for (int i = 0; i < 500; i++) {
			TaskFactory.Selection selection = TaskFactory.next(null, random, java.util.Set.of(), true, id -> true,
				DifficultyMix.WARM_UP);
			assertTrue(selection.difficulty().ordinal() <= TaskDifficulty.EASY.ordinal(), selection.id());
		}
	}

	@Test
	void tiersAreDrawnInTheModesProportions() {
		Random random = new Random(11);
		java.util.Map<DifficultyMix.Tier, Integer> counts = new java.util.EnumMap<>(DifficultyMix.Tier.class);
		int draws = 20000;
		for (int i = 0; i < draws; i++) {
			TaskFactory.Selection selection = TaskFactory.next(null, random, java.util.Set.of(), true, id -> true,
				MatchDifficulty.HARD.mix());
			counts.merge(DifficultyMix.Tier.of(selection.difficulty()), 1, Integer::sum);
		}
		assertEquals(0.30, counts.get(DifficultyMix.Tier.SIMPLE_OR_EASY) / (double) draws, 0.02);
		assertEquals(0.45, counts.get(DifficultyMix.Tier.MEDIUM) / (double) draws, 0.02);
		assertEquals(0.25, counts.get(DifficultyMix.Tier.HARD) / (double) draws, 0.02);
	}

	@Test
	void unavailableTiersGiveTheirShareToTheOthers() {
		Random random = new Random(3);
		for (int i = 0; i < 200; i++) {
			TaskFactory.Selection selection = TaskFactory.next(null, random, java.util.Set.of(), true,
				id -> TaskDifficulty.of(id) != TaskDifficulty.HARD, MatchDifficulty.HARD.mix());
			assertTrue(selection.difficulty() != TaskDifficulty.HARD, selection.id());
		}
	}

	@Test
	void weightedSelectionHonoursACustomWeight() {
		List<String> choices = List.of("never", "always");
		for (int i = 0; i < 50; i++) {
			assertEquals("always", TaskSelection.chooseWeighted(new Random(i), choices,
				choice -> choice.equals("always") ? 1.0 : 0.0));
		}
	}
}
