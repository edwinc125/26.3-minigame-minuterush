package com.edwinc125.minuterush.game;

/** Tracks a match pause without undoing a freeze that was already active. */
final class EmptyMatchPause {
	private boolean paused;
	private boolean ownsFreeze;

	boolean pause(boolean alreadyFrozen) {
		if (paused) return false;
		paused = true;
		ownsFreeze = !alreadyFrozen;
		return ownsFreeze;
	}

	boolean resume() {
		boolean unfreeze = ownsFreeze;
		paused = false;
		ownsFreeze = false;
		return unfreeze;
	}

	boolean isPaused() {
		return paused;
	}
}
