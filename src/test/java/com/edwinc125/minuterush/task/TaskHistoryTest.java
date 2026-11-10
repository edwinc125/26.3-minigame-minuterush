package com.edwinc125.minuterush.task;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class TaskHistoryTest {
	private final TaskHistory history = new TaskHistory();

	private List<String> eligible(String... candidates) {
		return TaskSelection.eligible(List.of(candidates), Function.identity(), history.completed(), history.recent());
	}

	@Test
	void skippedTasksWaitForFiveOtherTasks() {
		history.selected("a");
		for (String id : List.of("b", "c", "d", "e")) history.selected(id);
		assertEquals(List.of("g"), eligible("a", "g"));
		history.selected("f");
		assertEquals(List.of("a", "g"), eligible("a", "g"));
	}

	@Test
	void completedTasksRemainExcludedUntilAllPossibleTasksAreCompleted() {
		history.selected("a");
		history.completed("a");
		for (String id : List.of("b", "c", "d", "e", "f")) history.selected(id);
		assertEquals(List.of("g"), eligible("a", "g"));
		history.completed("g");
		assertEquals(List.of("a", "g"), eligible("a", "g"));
	}

	@Test
	void relaxesCooldownBeforeAllowingCompletedTasks() {
		history.selected("a");
		history.completed("b");
		assertEquals(List.of("a"), eligible("a", "b"));
		history.completed("a");
		assertEquals(List.of("b"), eligible("a", "b"));
		history.selected("b");
		assertEquals(List.of("a", "b"), eligible("a", "b"));
	}

	@Test
	void clearResetsBothKindsOfHistory() {
		history.selected("a");
		history.completed("a");
		history.clear();
		assertEquals(Set.of(), history.recent());
		assertEquals(Set.of(), history.completed());
	}
}
