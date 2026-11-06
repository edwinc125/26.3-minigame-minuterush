package com.edwinc125.minuterush.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class PlayerTaskTest {
	@Test
	void tasksInvolvingAnotherPlayerAreRecognized() {
		for (String id : new String[] {"hit-player", "kill-player", "interact-player", "hit-player-sword",
				"hit-player-axe", "hit-player-shovel", "hit-player-pickaxe", "hit-player-hoe",
				"hit-player-spear", "hit-player-fishing-rod"}) {
			assertTrue(TaskFactory.isPlayerTask(id), id);
		}
	}

	@Test
	void otherTasksAreNotPlayerTasks() {
		for (String id : new String[] {"hit-cow", "kill-zombie", "interact-mob", "interact-block",
				"use-fishing-rod", "craft-iron-sword"}) {
			assertFalse(TaskFactory.isPlayerTask(id), id);
		}
		assertFalse(TaskFactory.isPlayerTask(null));
	}
}
