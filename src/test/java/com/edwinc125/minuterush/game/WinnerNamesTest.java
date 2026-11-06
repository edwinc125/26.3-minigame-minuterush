package com.edwinc125.minuterush.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;

class WinnerNamesTest {
	@Test
	void joinsOneTwoOrManyNames() {
		assertEquals("Alex", WinnerNames.join(List.of("Alex")));
		assertEquals("Alex and Sam", WinnerNames.join(List.of("Alex", "Sam")));
		assertEquals("Alex, Sam and Kai", WinnerNames.join(List.of("Alex", "Sam", "Kai")));
		assertEquals("", WinnerNames.join(List.of()));
	}

	@Test
	void verbAgreesWithNumberOfWinners() {
		assertEquals("wins", WinnerNames.verb(List.of("Alex")));
		assertEquals("win", WinnerNames.verb(List.of("Alex", "Sam")));
	}
}
