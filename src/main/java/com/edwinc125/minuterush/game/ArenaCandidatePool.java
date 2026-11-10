package com.edwinc125.minuterush.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToDoubleFunction;

final class ArenaCandidatePool<T> {
	private final List<T> candidates = new ArrayList<>();
	private final Set<Long> inUse = new HashSet<>();
	private int generation;
	private int inFlight;

	void reset() {
		generation++;
		candidates.clear();
		inUse.clear();
		inFlight = 0;
	}

	void discardCandidates() {
		candidates.clear();
	}

	int generation() {
		return generation;
	}

	boolean reserve(long center) {
		if (!inUse.add(center)) {
			return false;
		}
		inFlight++;
		return true;
	}

	boolean complete(int scanGeneration, long center, T candidate) {
		if (scanGeneration != generation) {
			return false;
		}
		inFlight--;
		if (candidate == null) {
			inUse.remove(center);
		} else {
			candidates.add(candidate);
		}
		return true;
	}

	int size() {
		return candidates.size();
	}

	int inFlight() {
		return inFlight;
	}

	List<T> candidates() {
		return List.copyOf(candidates);
	}

	T removeBest(ToDoubleFunction<T> score) {
		T best = candidates.stream().max(Comparator.comparingDouble(score)).orElseThrow();
		candidates.remove(best);
		return best;
	}
}
