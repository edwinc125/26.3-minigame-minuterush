package com.edwinc125.minuterush.task;

final class CrouchProgress {
	private final int target;
	private boolean previous;
	private int count;

	CrouchProgress(int target, boolean crouching) {
		if (target <= 0) throw new IllegalArgumentException("Crouch target must be positive");
		this.target = target;
		this.previous = crouching;
	}

	boolean update(boolean crouching) {
		if (crouching && !previous) count++;
		previous = crouching;
		return count >= target;
	}
}
