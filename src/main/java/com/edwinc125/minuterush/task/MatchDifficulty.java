package com.edwinc125.minuterush.task;

import java.util.Locale;

/**
 * The difficulty setting for a whole match (discussion #28): how often Simple/Easy, Medium and Hard tasks come up.
 * {@link #PROGRESSIVE} moves from Easy to Medium to Hard as the match goes on (see {@link DifficultyProgress}).
 */
public enum MatchDifficulty {
	EASY(new DifficultyMix(0.60, 0.30, 0.10)),
	MEDIUM(new DifficultyMix(0.40, 0.40, 0.20)),
	HARD(new DifficultyMix(0.30, 0.45, 0.25)),
	PROGRESSIVE(null);

	private final DifficultyMix mix;

	MatchDifficulty(DifficultyMix mix) {
		this.mix = mix;
	}

	/** The fixed mix for this mode; null for {@link #PROGRESSIVE}, which borrows the mix of its current phase. */
	public DifficultyMix mix() {
		return mix;
	}

	/** Lower-case name used in commands, e.g. "progressive". */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** Parses a command argument such as "hard"; returns null if it is not a mode. */
	public static MatchDifficulty parse(String id) {
		for (MatchDifficulty difficulty : values()) {
			if (difficulty.id().equalsIgnoreCase(id)) {
				return difficulty;
			}
		}
		return null;
	}
}
