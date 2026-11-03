package com.edwinc125.minuterush.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class CollectProgressTest {
	@Test
	void requiredAmountIsAddedToTheRoundBaseline() {
		assertEquals(4, CollectProgress.requiredTotal(3, 1));
		assertEquals(8, CollectProgress.requiredTotal(5, 3));
	}
}
