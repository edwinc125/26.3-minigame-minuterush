package com.edwinc125.minuterush.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class DifficultyProgressTest {
	private static final long MINUTE = 60 * 20;

	@Test
	void everyModeStartsWithThreeCompletedTasksPerPlayerOfWarmUp() {
		for (MatchDifficulty mode : MatchDifficulty.values()) {
			DifficultyProgress progress = new DifficultyProgress();
			for (int i = 0; i < 6; i++) {
				assertEquals(DifficultyMix.WARM_UP, progress.next(mode, 2, 0), mode + " task " + i);
				progress.taskCompleted();
			}
			assertEquals(mode == MatchDifficulty.PROGRESSIVE ? MatchDifficulty.MEDIUM.mix() : mode.mix(),
				progress.next(mode, 2, 0), mode + " after warm-up");
		}
	}

	@Test
	void roundsNobodyCompletesDoNotUseUpTheWarmUp() {
		DifficultyProgress progress = new DifficultyProgress();
		for (int i = 0; i < 10; i++) {
			assertEquals(DifficultyMix.WARM_UP, progress.next(MatchDifficulty.HARD, 1, i * 20), "unfinished round " + i);
		}
		for (int i = 0; i < 3; i++) progress.taskCompleted();
		assertEquals(MatchDifficulty.HARD.mix(), progress.next(MatchDifficulty.HARD, 1, 300));
	}

	@Test
	void warmUpEndsAfterFiveMinutesWhenThatComesFirst() {
		DifficultyProgress progress = new DifficultyProgress();
		progress.next(MatchDifficulty.HARD, 1, 0);
		assertEquals(DifficultyMix.WARM_UP, progress.next(MatchDifficulty.HARD, 1, 5 * MINUTE - 1));
		assertEquals(MatchDifficulty.HARD.mix(), progress.next(MatchDifficulty.HARD, 1, 5 * MINUTE));
	}

	@Test
	void progressiveMovesOnAfterCompletedTasksWhenTheyComeFirst() {
		DifficultyProgress progress = new DifficultyProgress();
		// 2 players: Easy for 6 completed tasks, Medium for the next 8, then Hard.
		for (int i = 0; i < 6; i++) {
			progress.next(MatchDifficulty.PROGRESSIVE, 2, i * 20);
			progress.taskCompleted();
		}
		for (int i = 0; i < 8; i++) {
			assertEquals(MatchDifficulty.MEDIUM.mix(), progress.next(MatchDifficulty.PROGRESSIVE, 2, 200 + i * 20));
			progress.taskCompleted();
		}
		assertEquals(MatchDifficulty.HARD.mix(), progress.next(MatchDifficulty.PROGRESSIVE, 2, 400));
	}

	@Test
	void roundsNobodyCompletesDoNotMoveProgressiveOn() {
		DifficultyProgress progress = new DifficultyProgress();
		// 1 player: 3 completed tasks end Easy. Ten timed-out or skipped rounds in between change nothing.
		for (int i = 0; i < 10; i++) progress.next(MatchDifficulty.PROGRESSIVE, 1, i * 20);
		assertEquals(MatchDifficulty.EASY, progress.progressivePhase());
		for (int i = 0; i < 2; i++) progress.taskCompleted();
		progress.next(MatchDifficulty.PROGRESSIVE, 1, 300);
		assertEquals(MatchDifficulty.EASY, progress.progressivePhase());
		progress.taskCompleted();
		assertEquals(MatchDifficulty.MEDIUM.mix(), progress.next(MatchDifficulty.PROGRESSIVE, 1, 320));
		assertEquals(MatchDifficulty.MEDIUM, progress.progressivePhase());
	}

	@Test
	void progressiveMovesOnAfterTimeWhenItComesFirst() {
		DifficultyProgress progress = new DifficultyProgress();
		// 10 players: the task limits (30, then 40) are far away, so the 5 and 7 minute limits apply.
		progress.next(MatchDifficulty.PROGRESSIVE, 10, 0);
		progress.next(MatchDifficulty.PROGRESSIVE, 10, 5 * MINUTE - 1);
		assertEquals(MatchDifficulty.EASY, progress.progressivePhase());
		progress.next(MatchDifficulty.PROGRESSIVE, 10, 5 * MINUTE + 30 * 20);
		assertEquals(MatchDifficulty.MEDIUM, progress.progressivePhase());
		// Medium's 7 minutes count from when Easy ran out (5:00), not from the pick that noticed it (5:30).
		progress.next(MatchDifficulty.PROGRESSIVE, 10, 12 * MINUTE - 1);
		assertEquals(MatchDifficulty.MEDIUM, progress.progressivePhase());
		progress.next(MatchDifficulty.PROGRESSIVE, 10, 12 * MINUTE);
		assertEquals(MatchDifficulty.HARD, progress.progressivePhase());
	}

	@Test
	void progressiveCanSkipStraightToHardAfterALongGap() {
		DifficultyProgress progress = new DifficultyProgress();
		progress.next(MatchDifficulty.PROGRESSIVE, 1, 0);
		progress.next(MatchDifficulty.PROGRESSIVE, 1, 0);
		progress.next(MatchDifficulty.PROGRESSIVE, 1, 0);
		progress.next(MatchDifficulty.PROGRESSIVE, 1, 20 * MINUTE);
		assertEquals(MatchDifficulty.HARD, progress.progressivePhase());
	}

	@Test
	void resetStartsTheMatchOver() {
		DifficultyProgress progress = new DifficultyProgress();
		for (int i = 0; i < 20; i++) progress.next(MatchDifficulty.PROGRESSIVE, 1, 20 * MINUTE);
		progress.reset();
		assertEquals(MatchDifficulty.EASY, progress.progressivePhase());
		assertEquals(DifficultyMix.WARM_UP, progress.next(MatchDifficulty.HARD, 1, 0));
	}

	@Test
	void modesParseFromCommandIds() {
		assertEquals(MatchDifficulty.PROGRESSIVE, MatchDifficulty.parse("progressive"));
		assertEquals(MatchDifficulty.HARD, MatchDifficulty.parse("HARD"));
		assertNull(MatchDifficulty.parse("extreme"));
	}
}
