package com.edwinc125.minuterush.task;

/**
 * Tracks how far a match has come, to decide the {@link DifficultyMix} for each new task (discussion #28).
 *
 * <ul>
 *   <li>Whatever the mode, the first 3N tasks are Simple or Easy only, so players can gather resources.</li>
 *   <li>{@link MatchDifficulty#PROGRESSIVE} plays as Easy for 5 minutes or 3N tasks, then as Medium for 7 minutes or
 *       4N tasks (whichever comes first in each case), then as Hard for the rest of the match.</li>
 * </ul>
 * N is the number of players in the match (spectators excluded, briefly disconnected players included). The
 * progressive phase is tracked in every mode, so switching to progressive mid-match picks up where the match is.
 */
public final class DifficultyProgress {
	static final int WARM_UP_TASKS_PER_PLAYER = 3;
	static final int EASY_PHASE_TASKS_PER_PLAYER = 3;
	static final int MEDIUM_PHASE_TASKS_PER_PLAYER = 4;
	static final long EASY_PHASE_TICKS = 5 * 60 * 20;
	static final long MEDIUM_PHASE_TICKS = 7 * 60 * 20;

	private int tasksPicked;
	private MatchDifficulty phase = MatchDifficulty.EASY;
	private long phaseStartTick;
	private int phaseStartTasks;

	public void reset() {
		tasksPicked = 0;
		phase = MatchDifficulty.EASY;
		phaseStartTick = 0;
		phaseStartTasks = 0;
	}

	/**
	 * Returns the mix for the next task and counts it as picked.
	 *
	 * @param players      players in the match, spectators excluded
	 * @param elapsedTicks game ticks since the first round of the match started
	 */
	public DifficultyMix next(MatchDifficulty mode, int players, long elapsedTicks) {
		int n = Math.max(1, players);
		advancePhase(n, elapsedTicks);
		MatchDifficulty effective = mode == MatchDifficulty.PROGRESSIVE ? phase : mode;
		DifficultyMix mix = tasksPicked < WARM_UP_TASKS_PER_PLAYER * n ? DifficultyMix.WARM_UP : effective.mix();
		tasksPicked++;
		return mix;
	}

	/** The mode progressive play is currently in (Easy, Medium or Hard). */
	public MatchDifficulty progressivePhase() {
		return phase;
	}

	private void advancePhase(int n, long now) {
		if (phase == MatchDifficulty.EASY) {
			if (!endPhase(now, EASY_PHASE_TICKS, EASY_PHASE_TASKS_PER_PLAYER * n, MatchDifficulty.MEDIUM)) {
				return;
			}
		}
		if (phase == MatchDifficulty.MEDIUM) {
			endPhase(now, MEDIUM_PHASE_TICKS, MEDIUM_PHASE_TASKS_PER_PLAYER * n, MatchDifficulty.HARD);
		}
	}

	/** Moves to {@code nextPhase} if the current phase has used up its time or its tasks; returns whether it did. */
	private boolean endPhase(long now, long phaseTicks, int phaseTasks, MatchDifficulty nextPhase) {
		boolean outOfTime = now - phaseStartTick >= phaseTicks;
		boolean outOfTasks = tasksPicked - phaseStartTasks >= phaseTasks;
		if (!outOfTime && !outOfTasks) {
			return false;
		}
		// If time ran out, the next phase's clock starts when it did, not at this (later) pick.
		phaseStartTick = outOfTime ? phaseStartTick + phaseTicks : now;
		phaseStartTasks = tasksPicked;
		phase = nextPhase;
		return true;
	}
}
