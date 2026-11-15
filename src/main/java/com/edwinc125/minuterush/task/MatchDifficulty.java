package com.edwinc125.minuterush.task;

import java.util.Locale;
import net.minecraft.world.Difficulty;

/**
 * The difficulty setting for a whole match (discussion #28): how often Simple/Easy, Medium and Hard tasks come up.
 * {@link #PROGRESSIVE} moves from Easy to Medium to Hard as the match goes on (see {@link DifficultyProgress}).
 * Each mode also sets the Minecraft game difficulty (issue #60), so mobs get tougher along with the tasks.
 */
public enum MatchDifficulty {
	EASY(new DifficultyMix(0.60, 0.30, 0.10), Difficulty.EASY),
	MEDIUM(new DifficultyMix(0.40, 0.40, 0.20), Difficulty.NORMAL),
	HARD(new DifficultyMix(0.30, 0.45, 0.25), Difficulty.HARD),
	PROGRESSIVE(null, null);

	private final DifficultyMix mix;
	private final Difficulty gameDifficulty;

	MatchDifficulty(DifficultyMix mix, Difficulty gameDifficulty) {
		this.mix = mix;
		this.gameDifficulty = gameDifficulty;
	}

	/** The fixed mix for this mode; null for {@link #PROGRESSIVE}, which borrows the mix of its current phase. */
	public DifficultyMix mix() {
		return mix;
	}

	/**
	 * The Minecraft difficulty this mode plays at; null for {@link #PROGRESSIVE}, which borrows the difficulty of
	 * its current phase.
	 */
	public Difficulty gameDifficulty() {
		return gameDifficulty;
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
