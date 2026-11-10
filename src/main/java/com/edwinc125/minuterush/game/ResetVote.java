package com.edwinc125.minuterush.game;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

final class ResetVote {
	enum Result { ACCEPTED, PASSED, ALREADY_VOTED, NOT_ELIGIBLE }

	private final Set<UUID> votes = new HashSet<>();

	Result vote(UUID voter, Collection<UUID> eligible) {
		votes.retainAll(eligible);
		if (!eligible.contains(voter)) return Result.NOT_ELIGIBLE;
		if (!votes.add(voter)) return Result.ALREADY_VOTED;
		return passes(eligible) ? Result.PASSED : Result.ACCEPTED;
	}

	boolean passes(Collection<UUID> eligible) {
		votes.retainAll(eligible);
		return !eligible.isEmpty() && votes.size() == eligible.size();
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
