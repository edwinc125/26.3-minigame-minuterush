package com.edwinc125.minuterush.task;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/** Match-scoped task history, keyed by the catalog's stable task IDs. */
public final class TaskHistory {
	private static final int COOLDOWN_SIZE = 5;
	private final Set<String> completed = new HashSet<>();
	private final Deque<String> recent = new ArrayDeque<>();

	public void selected(String id) {
		recent.addLast(id);
		while (recent.size() > COOLDOWN_SIZE) recent.removeFirst();
	}

	public void completed(String id) {
		completed.add(id);
	}

	public Set<String> completed() {
		return Set.copyOf(completed);
	}

	public Set<String> recent() {
		return Set.copyOf(recent);
	}

	public void clear() {
		completed.clear();
		recent.clear();
	}
}
