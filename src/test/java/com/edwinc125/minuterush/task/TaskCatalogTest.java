package com.edwinc125.minuterush.task;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TaskCatalogTest {
	@Test
	void recentTaskIdsAreRecognizedByTheSelectionPool() {
		assertTrue(TaskSelection.isRecent("hold-tool", Set.of("hold-tool")));
	}

}
