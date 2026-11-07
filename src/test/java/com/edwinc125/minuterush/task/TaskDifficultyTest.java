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
	void stagesAdvanceEveryThreeTasksPerPlayer() {
		assertEquals(0, TaskDifficulty.stage(0, 2));
		assertEquals(0, TaskDifficulty.stage(5, 2));
		assertEquals(1, TaskDifficulty.stage(6, 2));
		assertEquals(2, TaskDifficulty.stage(12, 2));
		assertEquals(1, TaskDifficulty.stage(3, 1));
		assertEquals(1, TaskDifficulty.stage(3, 0), "zero players counts as one");
	}

	@Test
	void harderTiersUnlockGradually() {
		assertEquals(1.0, TaskDifficulty.SIMPLE.multiplier(0));
		assertEquals(1.0, TaskDifficulty.EASY.multiplier(0));
		assertEquals(0.0, TaskDifficulty.MEDIUM.multiplier(0));
		assertEquals(0.5, TaskDifficulty.MEDIUM.multiplier(1));
		assertEquals(1.0, TaskDifficulty.MEDIUM.multiplier(2));
		assertEquals(0.0, TaskDifficulty.HARD.multiplier(1));
		assertEquals(0.5, TaskDifficulty.HARD.multiplier(2));
		assertEquals(1.0, TaskDifficulty.HARD.multiplier(3));
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
	void theFirstStageOnlyDrawsSimpleAndEasyTasks() {
		Random random = new Random(7);
		for (int i = 0; i < 500; i++) {
			TaskFactory.Selection selection = TaskFactory.next(null, random, java.util.Set.of(), true, id -> true, 0);
			assertTrue(selection.difficulty().ordinal() <= TaskDifficulty.EASY.ordinal(), selection.id());
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
