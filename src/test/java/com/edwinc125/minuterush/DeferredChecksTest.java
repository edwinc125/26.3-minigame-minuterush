package com.edwinc125.minuterush;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeferredChecksTest {
	@Test
	void checksRunOnlyWhenTheTickEnds() {
		DeferredChecks checks = new DeferredChecks();
		List<String> ran = new ArrayList<>();
		checks.add(() -> ran.add("leash"));
		checks.add(() -> ran.add("door"));
		assertEquals(List.of(), ran, "checks must not run when queued");
		checks.runAll();
		assertEquals(List.of("leash", "door"), ran);
		checks.runAll();
		assertEquals(List.of("leash", "door"), ran, "each check runs once");
	}

	@Test
	void checksQueuedWhileRunningWaitForTheNextTick() {
		DeferredChecks checks = new DeferredChecks();
		List<String> ran = new ArrayList<>();
		checks.add(() -> checks.add(() -> ran.add("later")));
		checks.runAll();
		assertEquals(List.of(), ran);
		checks.runAll();
		assertEquals(List.of("later"), ran);
	}

	@Test
	void clearDropsPendingChecks() {
		DeferredChecks checks = new DeferredChecks();
		List<String> ran = new ArrayList<>();
		checks.add(() -> ran.add("x"));
		checks.clear();
		checks.runAll();
		assertEquals(List.of(), ran);
	}
}
