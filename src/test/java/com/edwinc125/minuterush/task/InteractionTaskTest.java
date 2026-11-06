package com.edwinc125.minuterush.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class InteractionTaskTest {
	@Test
	void anyMobInteractionAcceptsTheConcreteMobId() {
		InteractionTask task = new InteractionTask(InteractionTask.Kind.MOB, "any-mob", 1);
		assertTrue(task.accepts("zombie"));
	}

	@Test
	void anyBlockOnlyAcceptsInteractableBlocks() {
		InteractionTask task = new InteractionTask(InteractionTask.Kind.BLOCK, "any-block", 1);
		assertTrue(task.accepts("crafting_table"));
		assertTrue(task.accepts("chest"));
		assertTrue(task.accepts("oak_door"));
		assertTrue(task.accepts("red_bed"));
		assertTrue(task.accepts("spruce_fence_gate"));
		assertTrue(task.accepts("chipped_anvil"));
		assertFalse(task.accepts("air"));
		assertFalse(task.accepts("grass_block"));
		assertFalse(task.accepts("dirt"));
		assertFalse(task.accepts("stone"));
		assertFalse(task.accepts("iron_door"));
		assertFalse(task.accepts("iron_trapdoor"));
	}

	@Test
	void onlyPressurePlateTaskCountsStandingOnABlock() {
		assertTrue(new InteractionTask(InteractionTask.Kind.BLOCK, "pressure_plate", 1).countsStandingOn());
		assertFalse(new InteractionTask(InteractionTask.Kind.BLOCK, "any-block", 1).countsStandingOn());
		assertFalse(new InteractionTask(InteractionTask.Kind.BLOCK, "chest", 1).countsStandingOn());
		assertFalse(new InteractionTask(InteractionTask.Kind.MOB, "any-mob", 1).countsStandingOn());
		assertFalse(new InteractionTask(InteractionTask.Kind.PLAYER, "player", 1).countsStandingOn());
	}
}
