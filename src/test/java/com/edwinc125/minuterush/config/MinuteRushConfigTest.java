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

	@Test
	void summarizesTheSettingsThatWillBeUsedForTheMatch() {
		assertEquals("Match settings: difficulty progressive, PvP on, points to win 10, time per round 60s.",
			MinuteRushConfig.DEFAULT.matchStartSummary());
		assertEquals("Match settings: difficulty hard, PvP off, points to win 8, time per round 45s.",
			MinuteRushConfig.DEFAULT.withDifficulty(com.edwinc125.minuterush.task.MatchDifficulty.HARD)
				.withPvp(false).withTargetScore(8).withRoundDurationSeconds(45).matchStartSummary());
	}
}
