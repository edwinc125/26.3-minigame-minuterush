package com.edwinc125.minuterush.task;

import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.List;
import java.util.function.Function;

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

	/** Prefer uncompleted, cooled-down tasks; only relax restrictions when necessary. */
	public static <T> List<T> eligible(List<T> candidates, Function<T, String> id,
			Set<String> completed, Set<String> recent) {
		List<T> uncompleted = candidates.stream().filter(candidate -> !completed.contains(id.apply(candidate))).toList();
		List<T> preferred = uncompleted.stream().filter(candidate -> !recent.contains(id.apply(candidate))).toList();
		if (!preferred.isEmpty()) return preferred;
		if (!uncompleted.isEmpty()) return uncompleted;
		List<T> cooledDown = candidates.stream().filter(candidate -> !recent.contains(id.apply(candidate))).toList();
		return cooledDown.isEmpty() ? candidates : cooledDown;
	}

	public static <T extends Weighted> T chooseWeighted(RandomGenerator random, List<T> candidates) {
		return chooseWeighted(random, candidates, Weighted::weight);
	}

	public static <T> T chooseWeighted(RandomGenerator random, List<T> candidates,
			java.util.function.ToDoubleFunction<? super T> weight) {
		if (candidates.isEmpty()) {
			throw new IllegalArgumentException("Weighted pool must not be empty");
		}
		double total = candidates.stream().mapToDouble(weight).sum();
		double pick = random.nextDouble(total);
		for (T candidate : candidates) {
			pick -= weight.applyAsDouble(candidate);
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
