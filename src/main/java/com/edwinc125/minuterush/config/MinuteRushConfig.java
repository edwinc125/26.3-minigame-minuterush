package com.edwinc125.minuterush.config;

import com.edwinc125.minuterush.task.MatchDifficulty;

public record MinuteRushConfig(
		int targetScore,
		int roundDurationSeconds,
		int preGameCountdownSeconds,
		int interRoundBreakSeconds,
		int arenaChunkWidth,
		int arenaChunkDepth,
		int borderExpansionBlocksPerSecond,
		int maxBorderSize,
		int keepInventoryGraceSeconds,
		MatchDifficulty difficulty) {
	public static final MinuteRushConfig DEFAULT = new MinuteRushConfig(10, 60);

	public MinuteRushConfig(int targetScore, int roundDurationSeconds) {
		this(targetScore, roundDurationSeconds, 5, 5, 20, 20, 5, 1000, 5, MatchDifficulty.PROGRESSIVE);
	}

	public MinuteRushConfig {
		if (targetScore < 1 || roundDurationSeconds < 1 || preGameCountdownSeconds < 1
				|| interRoundBreakSeconds < 1 || arenaChunkWidth < 1 || arenaChunkDepth < 1
				|| borderExpansionBlocksPerSecond < 1 || maxBorderSize < 1
				|| keepInventoryGraceSeconds < 1) {
			throw new IllegalArgumentException("Minute Rush settings must be positive");
		}
		if (difficulty == null) {
			throw new IllegalArgumentException("Minute Rush difficulty must be set");
		}
	}

	public MinuteRushConfig withTargetScore(int targetScore) {
		return new MinuteRushConfig(targetScore, roundDurationSeconds, preGameCountdownSeconds,
			interRoundBreakSeconds, arenaChunkWidth, arenaChunkDepth, borderExpansionBlocksPerSecond,
			maxBorderSize, keepInventoryGraceSeconds, difficulty);
	}

	public MinuteRushConfig withRoundDurationSeconds(int roundDurationSeconds) {
		return new MinuteRushConfig(targetScore, roundDurationSeconds, preGameCountdownSeconds,
			interRoundBreakSeconds, arenaChunkWidth, arenaChunkDepth, borderExpansionBlocksPerSecond,
			maxBorderSize, keepInventoryGraceSeconds, difficulty);
	}

	public MinuteRushConfig withDifficulty(MatchDifficulty difficulty) {
		return new MinuteRushConfig(targetScore, roundDurationSeconds, preGameCountdownSeconds,
			interRoundBreakSeconds, arenaChunkWidth, arenaChunkDepth, borderExpansionBlocksPerSecond,
			maxBorderSize, keepInventoryGraceSeconds, difficulty);
	}
}
