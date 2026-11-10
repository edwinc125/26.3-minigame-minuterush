package com.edwinc125.minuterush.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArenaCandidateScoreTest {
	@Test
	void equallyWeightsNormalizedBiomeStructureAndLandScores() {
		ArenaCandidateScore score = ArenaCandidateScore.fromSamples(12, 20, 1, 4).orElseThrow();

		assertEquals(0.75, score.landRatio());
		assertEquals(50.0, score.score(), 0.0001);
	}

	@Test
	void acceptsExactlyHalfLandAndRejectsMoreThanHalfWater() {
		assertTrue(ArenaCandidateScore.fromSamples(12, 20, 2, 4).isPresent());
		assertFalse(ArenaCandidateScore.fromSamples(12, 20, 3, 4).isPresent());
	}

	@Test
	void capsComponentScoresAndUsesBenchmarkedLowScoreCutoff() {
		ArenaCandidateScore score = ArenaCandidateScore.fromSamples(48, 80, 0, 4).orElseThrow();
		assertEquals(100.0, score.score(), 0.0001);
		assertTrue(ArenaCandidateScore.fromSamples(12, 20, 2, 4).orElseThrow().isLow());
		assertFalse(ArenaCandidateScore.fromSamples(24, 40, 1, 2).orElseThrow().isLow());
		assertTrue(new ArenaCandidateScore(0, 0, 0.5, 52.999).isLow());
		assertFalse(new ArenaCandidateScore(0, 0, 0.5, 53.0).isLow());
	}

	@Test
	void reevaluatesWhenAtLeastFourOfFiveCandidatesAreLow() {
		ArenaCandidateScore low = ArenaCandidateScore.fromSamples(12, 20, 2, 4).orElseThrow();
		ArenaCandidateScore high = ArenaCandidateScore.fromSamples(24, 40, 0, 2).orElseThrow();

		assertFalse(ArenaCandidateScore.shouldReevaluatePool(List.of(low, low, low, high)));
		assertFalse(ArenaCandidateScore.shouldReevaluatePool(List.of(low, low, high, high, high)));
		assertTrue(ArenaCandidateScore.shouldReevaluatePool(List.of(low, low, low, low, high)));
		assertTrue(ArenaCandidateScore.shouldReevaluatePool(List.of(low, low, low, low, low)));
	}
}
