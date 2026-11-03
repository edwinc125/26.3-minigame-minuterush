package com.edwinc125.minuterush.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class TaskBalanceTest {
	@Test
	void actionMultipliersAreAppliedOnTopOfRarity() {
		TaskFactory.Selection crafting = new TaskFactory.Selection("craft", null, 1.0, "crafting");
		TaskFactory.Selection movement = new TaskFactory.Selection("move", null, 1.0, "movement");
		TaskFactory.Selection hold = new TaskFactory.Selection("hold", null, 1.0, "hold");
		assertEquals(1.50, crafting.weight());
		assertEquals(1.40, movement.weight());
		assertEquals(0.40, hold.weight());
	}

	@Test
	void weightedSelectionIsDeterministicForSeed() {
		List<TaskFactory.Selection> choices = List.of(
			new TaskFactory.Selection("low", null, 0.5, "hold"),
			new TaskFactory.Selection("high", null, 1.5, "crafting"));
		assertEquals(
			TaskSelection.chooseWeighted(new Random(12), choices).id(),
			TaskSelection.chooseWeighted(new Random(12), choices).id());
	}

}
