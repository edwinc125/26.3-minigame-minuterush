package com.edwinc125.minuterush.game;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Tracks player votes to skip the current task. A vote passes when strictly more than half of eligible players agree. */
final class SkipVote {
	enum Result { ACCEPTED, PASSED, ALREADY_VOTED, NOT_ELIGIBLE }

	private final Set<UUID> votes = new HashSet<>();

	static int required(int eligibleCount) {
		return eligibleCount / 2 + 1;
	}

	Result vote(UUID voter, Collection<UUID> eligible) {
		if (!eligible.contains(voter)) {
			return Result.NOT_ELIGIBLE;
		}
		if (!votes.add(voter)) {
			return Result.ALREADY_VOTED;
		}
		return passes(eligible) ? Result.PASSED : Result.ACCEPTED;
	}

	/** Drops votes from players who are no longer eligible, then checks the threshold. */
	boolean passes(Collection<UUID> eligible) {
		votes.retainAll(eligible);
		return !eligible.isEmpty() && votes.size() >= required(eligible.size());
	}

	void remove(UUID voter) {
		votes.remove(voter);
	}

	int count() {
		return votes.size();
	}

	void clear() {
		votes.clear();
	}
}
