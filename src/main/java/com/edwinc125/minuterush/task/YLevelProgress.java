package com.edwinc125.minuterush.task;

public final class YLevelProgress {
	private final int targetLevel;
	private boolean hasLeftTarget;

	public YLevelProgress(int targetLevel, double startingY) {
		this.targetLevel = targetLevel;
		hasLeftTarget = floorLevel(startingY) != targetLevel;
	}

	public boolean update(double y) {
		int currentLevel = floorLevel(y);
		if (currentLevel != targetLevel) {
			hasLeftTarget = true;
		}
		return hasLeftTarget && currentLevel == targetLevel;
	}

	private static int floorLevel(double y) {
		return (int) Math.floor(y);
	}
}
