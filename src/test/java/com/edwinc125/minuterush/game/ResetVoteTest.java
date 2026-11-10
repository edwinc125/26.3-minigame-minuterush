package com.edwinc125.minuterush.game;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResetVoteTest {
	private final UUID a = UUID.randomUUID();
	private final UUID b = UUID.randomUUID();
	private final UUID c = UUID.randomUUID();

	@Test
	void requiresEveryConnectedParticipant() {
		ResetVote vote = new ResetVote();
		assertEquals(ResetVote.Result.ACCEPTED, vote.vote(a, List.of(a, b, c)));
		assertEquals(ResetVote.Result.ALREADY_VOTED, vote.vote(a, List.of(a, b, c)));
		assertEquals(ResetVote.Result.ACCEPTED, vote.vote(b, List.of(a, b, c)));
		assertEquals(ResetVote.Result.PASSED, vote.vote(c, List.of(a, b, c)));
	}

	@Test
	void rejectsSpectatorsAndEmptyElectorates() {
		ResetVote vote = new ResetVote();
		assertEquals(ResetVote.Result.NOT_ELIGIBLE, vote.vote(c, List.of(a, b)));
		assertFalse(vote.passes(List.of()));
		assertEquals(ResetVote.Result.PASSED, vote.vote(a, List.of(a)));
	}

	@Test
	void tracksJoiningAndLeavingParticipants() {
		ResetVote vote = new ResetVote();
		vote.vote(a, List.of(a, b));
		assertFalse(vote.passes(List.of(a, b, c)));
		assertTrue(vote.passes(List.of(a)));
		vote.remove(a);
		assertFalse(vote.passes(List.of(b)));
		assertEquals(ResetVote.Result.ACCEPTED, vote.vote(b, List.of(a, b)));
		vote.clear();
		assertEquals(0, vote.count());
	}
}
