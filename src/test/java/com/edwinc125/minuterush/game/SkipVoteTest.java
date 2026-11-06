package com.edwinc125.minuterush.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SkipVoteTest {
	private final UUID a = UUID.randomUUID();
	private final UUID b = UUID.randomUUID();
	private final UUID c = UUID.randomUUID();
	private final UUID d = UUID.randomUUID();

	@Test
	void requiresStrictlyMoreThanHalf() {
		assertEquals(1, SkipVote.required(1));
		assertEquals(2, SkipVote.required(2));
		assertEquals(2, SkipVote.required(3));
		assertEquals(3, SkipVote.required(4));
		assertEquals(3, SkipVote.required(5));
	}

	@Test
	void soloVotePassesImmediately() {
		assertEquals(SkipVote.Result.PASSED, new SkipVote().vote(a, List.of(a)));
	}

	@Test
	void halfIsNotEnough() {
		SkipVote vote = new SkipVote();
		List<UUID> players = List.of(a, b, c, d);
		assertEquals(SkipVote.Result.ACCEPTED, vote.vote(a, players));
		assertEquals(SkipVote.Result.ACCEPTED, vote.vote(b, players));
		assertEquals(SkipVote.Result.PASSED, vote.vote(c, players));
	}

	@Test
	void twoOfThreePasses() {
		SkipVote vote = new SkipVote();
		List<UUID> players = List.of(a, b, c);
		assertEquals(SkipVote.Result.ACCEPTED, vote.vote(a, players));
		assertEquals(SkipVote.Result.PASSED, vote.vote(b, players));
	}

	@Test
	void duplicateVotesAreRejected() {
		SkipVote vote = new SkipVote();
		List<UUID> players = List.of(a, b, c);
		vote.vote(a, players);
		assertEquals(SkipVote.Result.ALREADY_VOTED, vote.vote(a, players));
		assertEquals(1, vote.count());
	}

	@Test
	void nonParticipantsCannotVote() {
		SkipVote vote = new SkipVote();
		assertEquals(SkipVote.Result.NOT_ELIGIBLE, vote.vote(d, List.of(a, b)));
		assertEquals(0, vote.count());
	}

	@Test
	void leavingNonVoterCanTipTheVote() {
		SkipVote vote = new SkipVote();
		vote.vote(a, List.of(a, b, c, d));
		vote.vote(b, List.of(a, b, c, d));
		assertFalse(vote.passes(List.of(a, b, c, d)));
		assertTrue(vote.passes(List.of(a, b, c)));
	}

	@Test
	void votesFromDepartedPlayersAreDropped() {
		SkipVote vote = new SkipVote();
		vote.vote(a, List.of(a, b, c));
		vote.remove(a);
		assertEquals(0, vote.count());
		vote.vote(b, List.of(a, b, c));
		assertFalse(vote.passes(List.of(b, c, d)));
		assertEquals(1, vote.count());
		// a is no longer eligible, so a stale vote from a is pruned rather than counted.
		vote.vote(a, List.of(a, b, c));
		assertTrue(vote.passes(List.of(a, b, c)));
		assertFalse(vote.passes(List.of(b, c, d)));
		assertEquals(1, vote.count());
	}

	@Test
	void clearResetsForNextTask() {
		SkipVote vote = new SkipVote();
		vote.vote(a, List.of(a, b, c));
		vote.clear();
		assertEquals(0, vote.count());
		assertEquals(SkipVote.Result.ACCEPTED, vote.vote(a, List.of(a, b, c)));
	}

	@Test
	void emptyElectorateNeverPasses() {
		assertFalse(new SkipVote().passes(List.of()));
	}
}
