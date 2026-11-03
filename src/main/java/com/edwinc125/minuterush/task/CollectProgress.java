package com.edwinc125.minuterush.task;

public final class CollectProgress {
	private CollectProgress() {}

	public static int requiredTotal(int baseline, int required) {
		if (baseline < 0 || required < 1) {
			throw new IllegalArgumentException("Baseline must be non-negative and required amount must be positive");
		}
		return baseline + required;
	}
}
