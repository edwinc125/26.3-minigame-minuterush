package com.edwinc125.minuterush.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScoreboardDisplayTest {
	@Test
	void listsParticipantsWhoAreNotSpectating() {
		assertTrue(ScoreboardDisplay.shouldList(true, false));
	}

	@Test
	void excludesSpectators() {
		assertFalse(ScoreboardDisplay.shouldList(true, true));
		assertFalse(ScoreboardDisplay.shouldList(false, true));
	}

	@Test
	void excludesNonParticipants() {
		assertFalse(ScoreboardDisplay.shouldList(false, false));
	}

	@Test
	void resetsTaskAndTimeLinesEveryRefresh() {
		List<String> reset = ScoreboardDisplay.entriesToReset(
			List.of("§bTask: Jump", "§eTime: 42s", "Alice"), Set.of("Alice"));
		assertEquals(List.of("§bTask: Jump", "§eTime: 42s"), reset);
	}

	@Test
	void removesPlayersWhoAreNoLongerListed() {
		List<String> reset = ScoreboardDisplay.entriesToReset(
			List.of("Alice", "Bob"), Set.of("Alice"));
		assertEquals(List.of("Bob"), reset);
	}

	@Test
	void keepsListedPlayers() {
		List<String> reset = ScoreboardDisplay.entriesToReset(
			List.of("Alice", "Bob"), Set.of("Alice", "Bob"));
		assertTrue(reset.isEmpty());
	}
}
