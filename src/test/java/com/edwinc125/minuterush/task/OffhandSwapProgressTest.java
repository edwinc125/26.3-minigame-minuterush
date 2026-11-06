package com.edwinc125.minuterush.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class OffhandSwapProgressTest {
	@Test
	void detectsItemsTradingHands() {
		OffhandSwapProgress progress = new OffhandSwapProgress();
		assertFalse(progress.update("stone", "torch", 0));
		assertTrue(progress.update("torch", "stone", 0));
	}

	@Test
	void detectsSwappingAnItemIntoAnEmptyOffhand() {
		OffhandSwapProgress progress = new OffhandSwapProgress();
		assertFalse(progress.update("stone", "air", 0));
		assertTrue(progress.update("air", "stone", 0));
	}

	@Test
	void emptyHandsNeverCountAsASwap() {
		// Regression for issue #24: empty hands (e.g. while crafting) matched every tick.
		OffhandSwapProgress progress = new OffhandSwapProgress();
		assertFalse(progress.update("air", "air", 0));
		assertFalse(progress.update("air", "air", 0));
		assertFalse(progress.update("air", "air", 0));
	}

	@Test
	void identicalItemsInBothHandsNeverCountAsASwap() {
		OffhandSwapProgress progress = new OffhandSwapProgress();
		assertFalse(progress.update("torch", "torch", 0));
		assertFalse(progress.update("torch", "torch", 0));
	}

	@Test
	void changingHotbarSlotIsNotASwap() {
		OffhandSwapProgress progress = new OffhandSwapProgress();
		assertFalse(progress.update("stone", "torch", 0));
		assertFalse(progress.update("torch", "stone", 1));
	}

	@Test
	void firstObservationIsNeverASwap() {
		OffhandSwapProgress progress = new OffhandSwapProgress();
		assertFalse(progress.update("torch", "stone", 0));
	}

	@Test
	void unchangedHandsAreNotASwap() {
		OffhandSwapProgress progress = new OffhandSwapProgress();
		assertFalse(progress.update("stone", "torch", 0));
		assertFalse(progress.update("stone", "torch", 0));
	}
}
