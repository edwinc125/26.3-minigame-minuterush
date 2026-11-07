package com.edwinc125.minuterush.task;

/**
 * How likely each tier of task is when a round's task is drawn. Shares are relative and need not add up to 1;
 * a tier with no task available in the arena drops out and the remaining tiers share its chance.
 */
public record DifficultyMix(double simpleOrEasy, double medium, double hard) {
	/** Used for the first 3N tasks of every match so players can gather resources first. */
	public static final DifficultyMix WARM_UP = new DifficultyMix(1, 0, 0);

	public DifficultyMix {
		if (simpleOrEasy < 0 || medium < 0 || hard < 0 || simpleOrEasy + medium + hard <= 0) {
			throw new IllegalArgumentException("Difficulty shares must be non-negative and not all zero");
		}
	}

	public double share(Tier tier) {
		return switch (tier) {
			case SIMPLE_OR_EASY -> simpleOrEasy;
			case MEDIUM -> medium;
			case HARD -> hard;
		};
	}

	/** Task difficulties grouped the way match difficulties weight them: Simple and Easy share one chance. */
	public enum Tier {
		SIMPLE_OR_EASY, MEDIUM, HARD;

		public static Tier of(TaskDifficulty difficulty) {
			return switch (difficulty) {
				case SIMPLE, EASY -> SIMPLE_OR_EASY;
				case MEDIUM -> MEDIUM;
				case HARD -> HARD;
			};
		}
	}
}
