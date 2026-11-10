package com.edwinc125.minuterush.game;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class EmptyMatchPauseTest {
	@Test
	void ownsAndReleasesAFreezeItStarted() {
		EmptyMatchPause pause = new EmptyMatchPause();
		assertFalse(pause.isPaused());
		assertTrue(pause.pause(false));
		assertTrue(pause.isPaused());
		assertFalse(pause.pause(true));
		assertTrue(pause.resume());
		assertFalse(pause.isPaused());
		assertFalse(pause.resume());
	}

	@Test
	void preservesAnExistingServerFreeze() {
		EmptyMatchPause pause = new EmptyMatchPause();
		assertFalse(pause.pause(true));
		assertTrue(pause.isPaused());
		assertFalse(pause.resume());
		assertFalse(pause.isPaused());
	}
}
