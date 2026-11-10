package com.edwinc125.minuterush.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArenaCandidatePoolTest {
	@Test
	void resetStartsFreshAndIgnoresLateResultsFromThePreviousMatch() {
		ArenaCandidatePool<Integer> pool = new ArenaCandidatePool<>();
		int oldGeneration = pool.generation();
		assertTrue(pool.reserve(1));
		assertTrue(pool.reserve(2));
		assertTrue(pool.complete(oldGeneration, 1, 100));

		pool.reset();
		assertTrue(pool.reserve(2));
		assertFalse(pool.complete(oldGeneration, 2, 99));
		assertEquals(1, pool.inFlight());
		assertEquals(0, pool.size());
		assertTrue(pool.complete(pool.generation(), 2, 10));
		assertEquals(10, pool.removeBest(Integer::doubleValue));
	}

	@Test
	void selectsTheHighestOfTheCurrentFiveCandidates() {
		ArenaCandidatePool<Integer> pool = new ArenaCandidatePool<>();
		for (int score : new int[] {30, 70, 50, 90, 60}) {
			assertTrue(pool.reserve(score));
			assertTrue(pool.complete(pool.generation(), score, score));
		}
		assertEquals(5, pool.size());
		assertEquals(90, pool.removeBest(Integer::doubleValue));
		assertEquals(70, pool.removeBest(Integer::doubleValue));
	}

	@Test
	void discardedCandidatesCannotBeResampledDuringReevaluation() {
		ArenaCandidatePool<Integer> pool = new ArenaCandidatePool<>();
		assertTrue(pool.reserve(1));
		assertTrue(pool.complete(pool.generation(), 1, 10));
		pool.discardCandidates();
		assertFalse(pool.reserve(1));
		assertTrue(pool.reserve(2));
		assertTrue(pool.complete(pool.generation(), 2, null));
		assertTrue(pool.reserve(2));
	}
}
