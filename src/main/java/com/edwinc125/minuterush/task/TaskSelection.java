package com.edwinc125.minuterush.task;

import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.List;

public final class TaskSelection {
	private TaskSelection() {}

	public static int chooseIndex(RandomGenerator random, int size, Set<Integer> recent) {
		if (size < 1) {
			throw new IllegalArgumentException("Task pool must not be empty");
		}
		if (recent.size() >= size) {
			return random.nextInt(size);
		}
		int candidate;
		do {
			candidate = random.nextInt(size);
		} while (recent.contains(candidate));
		return candidate;
	}

	public static boolean isRecent(String id, Set<String> recent) {
		return recent.contains(id);
	}

	public static <T extends Weighted> T chooseWeighted(RandomGenerator random, List<T> candidates) {
		if (candidates.isEmpty()) {
			throw new IllegalArgumentException("Weighted pool must not be empty");
		}
		double total = candidates.stream().mapToDouble(Weighted::weight).sum();
		double pick = random.nextDouble(total);
		for (T candidate : candidates) {
			pick -= candidate.weight();
			if (pick < 0) {
				return candidate;
			}
		}
		return candidates.get(candidates.size() - 1);
	}

	public interface Weighted {
		double weight();
	}
}
