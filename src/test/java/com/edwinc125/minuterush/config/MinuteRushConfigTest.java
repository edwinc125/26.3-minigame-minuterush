package com.edwinc125.minuterush.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class MinuteRushConfigTest {
	@Test
	void defaultsMatchTheGameRules() {
		assertEquals(10, MinuteRushConfig.DEFAULT.targetScore());
		assertEquals(60, MinuteRushConfig.DEFAULT.roundDurationSeconds());
		assertEquals(com.edwinc125.minuterush.task.MatchDifficulty.PROGRESSIVE, MinuteRushConfig.DEFAULT.difficulty());
	}

	@Test
	void rejectsNonPositiveSettings() {
		assertThrows(IllegalArgumentException.class, () -> new MinuteRushConfig(0, 60));
		assertThrows(IllegalArgumentException.class, () -> new MinuteRushConfig(10, 0));
	}
}
