package com.edwinc125.minuterush.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class TaskDescriptionTest {
	@Test
	void interactionLabelsAreHumanReadable() {
		String mob = new InteractionTask(InteractionTask.Kind.MOB, "any-mob", 1).description();
		String block = new InteractionTask(InteractionTask.Kind.BLOCK, "any-block", 1).description();
		assertTrue(mob.contains("any mob"));
		assertTrue(mob.contains("Use or hit"));
		assertTrue(block.contains("interactable block"));
		assertFalse(mob.contains("{minecraft:"));
		assertFalse(block.contains("{minecraft:"));
	}
}
