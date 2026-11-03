package com.edwinc125.minuterush.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class TitleTimingsTest {
	@Test
	void titlesAppearImmediately() {
		assertEquals(0, TitleTimings.FADE_IN_TICKS);
		assertEquals(40, TitleTimings.STAY_TICKS);
	}
}
