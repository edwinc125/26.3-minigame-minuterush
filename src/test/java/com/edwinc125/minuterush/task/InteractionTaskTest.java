package com.edwinc125.minuterush.task;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InteractionTaskTest {
	@Test
	void anyMobInteractionAcceptsTheConcreteMobId() {
		InteractionTask task = new InteractionTask(InteractionTask.Kind.MOB, "any-mob", 1);
		assertTrue(task.accepts("zombie"));
	}
}
