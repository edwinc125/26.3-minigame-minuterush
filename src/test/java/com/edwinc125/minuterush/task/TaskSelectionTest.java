package com.edwinc125.minuterush.task;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Set;
import java.util.Random;
import org.junit.jupiter.api.Test;

class TaskSelectionTest {
	@Test
	void avoidsRecentTaskKindsWhenPoolHasAlternatives() {
		assertNotEquals(2, TaskSelection.chooseIndex(new Random(4), 3, Set.of(2)));
	}

	@Test
	void rejectsAnEmptyPool() {
		assertThrows(IllegalArgumentException.class,
			() -> TaskSelection.chooseIndex(new Random(), 0, Set.of()));
	}
}
