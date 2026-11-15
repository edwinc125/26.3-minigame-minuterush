package com.edwinc125.minuterush.task;

import net.minecraft.world.Difficulty;

/**
 * Tracks how far a match has come, to decide the {@link DifficultyMix} for each new task (discussion #28).
 *
 * <ul>
 *   <li>Whatever the mode, tasks are Simple or Easy only for 5 minutes or until 3N have been completed (whichever
 *       comes first), so players can gather resources.</li>
 *   <li>{@link MatchDifficulty#PROGRESSIVE} plays as Easy for 5 minutes or 3N completed tasks, then as Medium for
 *       7 minutes or 4N completed tasks (whichever comes first in each case), then as Hard for the rest of the
 *       match.</li>
 * </ul>
 * The Minecraft game difficulty follows the same phases (issue #60): Easy during the warm-up whatever the mode, then
 * the mode's own difficulty (Easy, Normal or Hard), with progressive stepping through all three.
 * A task is completed when someone finishes its round; rounds that time out or are skipped don't count, and a round
 * several players finish on the same tick counts once.
 * N is the number of players in the match (spectators excluded, briefly disconnected players included). The
 * progressive phase is tracked in every mode, so switching to progressive mid-match picks up where the match is.
 */
public final class DifficultyProgress {
	static final int WARM_UP_TASKS_PER_PLAYER = 3;
	static final long WARM_UP_TICKS = 5 * 60 * 20;
	static final int EASY_PHASE_TASKS_PER_PLAYER = 3;
	static final int MEDIUM_PHASE_TASKS_PER_PLAYER = 4;
	static final long EASY_PHASE_TICKS = 5 * 60 * 20;
	static final long MEDIUM_PHASE_TICKS = 7 * 60 * 20;

	private int tasksCompleted;
	private MatchDifficulty phase = MatchDifficulty.EASY;
	private long phaseStartTick;
	private int phaseStartCompleted;
	private Difficulty gameDifficulty = Difficulty.EASY;

	public void reset() {
		tasksCompleted = 0;
		phase = MatchDifficulty.EASY;
		phaseStartTick = 0;
		phaseStartCompleted = 0;
		gameDifficulty = Difficulty.EASY;
	}

	/** Counts a round someone finished; the warm-up and progressive phases end after enough of these. */
	public void taskCompleted() {
		tasksCompleted++;
	}

	/**
	 * Returns the mix for the next task.
	 *
	 * @param players      players in the match, spectators excluded
	 * @param elapsedTicks game ticks since the first round of the match started
	 */
	public DifficultyMix next(MatchDifficulty mode, int players, long elapsedTicks) {
		int n = Math.max(1, players);
		advancePhase(n, elapsedTicks);
		MatchDifficulty effective = mode == MatchDifficulty.PROGRESSIVE ? phase : mode;
		boolean warmUp = tasksCompleted < WARM_UP_TASKS_PER_PLAYER * n && elapsedTicks < WARM_UP_TICKS;
		gameDifficulty = warmUp ? Difficulty.EASY : effective.gameDifficulty();
		return warmUp ? DifficultyMix.WARM_UP : effective.mix();
	}

	/**
	 * The Minecraft difficulty that goes with the last {@link #next} pick: Easy during the warm-up (and before the
	 * first pick, so the countdown and first round are Easy), otherwise the effective mode's difficulty. It only
	 * changes when a new task is picked, so mobs don't get tougher partway through a round.
	 */
	public Difficulty gameDifficulty() {
		return gameDifficulty;
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

	/**
	 * Moves to {@code nextPhase} if the current phase has used up its time or had enough tasks completed; returns
	 * whether it did.
	 */
	private boolean endPhase(long now, long phaseTicks, int phaseTasks, MatchDifficulty nextPhase) {
		boolean outOfTime = now - phaseStartTick >= phaseTicks;
		boolean outOfTasks = tasksCompleted - phaseStartCompleted >= phaseTasks;
		if (!outOfTime && !outOfTasks) {
			return false;
		}
		// If time ran out, the next phase's clock starts when it did, not at this (later) pick.
		phaseStartTick = outOfTime ? phaseStartTick + phaseTicks : now;
		phaseStartCompleted = tasksCompleted;
		phase = nextPhase;
		return true;
	}
}
