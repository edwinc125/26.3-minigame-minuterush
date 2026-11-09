package com.edwinc125.minuterush.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class LobbyCountdownPolicyTest {
	@Test
	void roundsOddPlayerCountsUpAndEvenCountsToHalf() {
		assertEquals(1, LobbyCountdownPolicy.requiredReady(1));
		assertEquals(1, LobbyCountdownPolicy.requiredReady(2));
		assertEquals(2, LobbyCountdownPolicy.requiredReady(3));
		assertEquals(2, LobbyCountdownPolicy.requiredReady(4));
		assertEquals(3, LobbyCountdownPolicy.requiredReady(5));
		assertEquals(3, LobbyCountdownPolicy.requiredReady(6));
		assertEquals(4, LobbyCountdownPolicy.requiredReady(7));
	}

	@Test
	void usesStartingTextAtFiveSecondsOrLess() {
		assertEquals("Starting in 5s", LobbyCountdownPolicy.bossbarText(5 * 20));
		assertEquals("Starting in 1s", LobbyCountdownPolicy.bossbarText(1));
	}

	@Test
	void usesWaitingTextAboveFiveSeconds() {
		assertEquals("Waiting for more players to be ready... | 30s",
			LobbyCountdownPolicy.bossbarText(30 * 20));
	}
}
