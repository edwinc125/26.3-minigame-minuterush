package com.edwinc125.minuterush;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Checks that must run after vanilla has finished handling an interaction, e.g. "is the mob leashed now?".
 *
 * <p>Interaction callbacks fire before vanilla acts. {@code server.execute(...)} does not defer them: since 26.x
 * packets are handled by a separate packet processor rather than as server tasks, so {@code execute} called from
 * a packet handler runs the check immediately, before the interaction happens. Instead, checks queued here run at
 * the end of the server tick, after that tick's packets have been handled.
 */
public final class DeferredChecks {
	private final Queue<Runnable> pending = new ArrayDeque<>();

	public void add(Runnable check) {
		pending.add(check);
	}

	/** Runs the checks queued so far. Checks queued while running wait for the next call. */
	public void runAll() {
		for (int remaining = pending.size(); remaining > 0; remaining--) {
			pending.remove().run();
		}
	}

	public void clear() {
		pending.clear();
	}
}
