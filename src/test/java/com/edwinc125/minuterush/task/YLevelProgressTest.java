package com.edwinc125.minuterush.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class YLevelProgressTest {
	@Test
	void requiresEnteringTheExactFlooredLevelFromAnotherLevel() {
		YLevelProgress progress = new YLevelProgress(100, 99.9);
		assertTrue(progress.update(100.9));
	}

	@Test
	void reachingANeighboringLevelDoesNotComplete() {
		YLevelProgress progress = new YLevelProgress(100, 101.0);
		assertFalse(progress.update(101.9));
		assertFalse(progress.update(99.9));
		assertTrue(progress.update(100.0));
	}

	@Test
	void mustLeaveAndReenterWhenStartingOnTheTargetLevel() {
		YLevelProgress progress = new YLevelProgress(100, 100.8);
		assertFalse(progress.update(100.2));
		assertFalse(progress.update(99.9));
		assertTrue(progress.update(100.0));
	}

	@Test
	void floorsNegativeCoordinates() {
		YLevelProgress progress = new YLevelProgress(-1, -2.0);
		assertFalse(progress.update(-1.1));
		assertTrue(progress.update(-1.0));
	}
}
