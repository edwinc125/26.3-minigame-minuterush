package com.edwinc125.minuterush.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class EntityTaskProgressTest {
	@Test
	void useEventMatchesHyphenatedTaskKeys() {
		assertEquals("use:flint-and-steel", EntityTaskProgress.useEvent("flint_and_steel"));
		assertEquals("use:fishing-rod", EntityTaskProgress.useEvent("fishing_rod"));
		assertEquals("use:shears", EntityTaskProgress.useEvent("shears"));
	}
}
