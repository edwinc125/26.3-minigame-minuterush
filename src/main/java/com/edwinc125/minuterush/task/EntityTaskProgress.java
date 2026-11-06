package com.edwinc125.minuterush.task;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EntityTaskProgress {
	private static final Map<UUID, Map<String, Integer>> COUNTS = new HashMap<>();

	private EntityTaskProgress() {}

	public static void record(UUID playerId, String event) {
		COUNTS.computeIfAbsent(playerId, ignored -> new HashMap<>())
			.merge(event, 1, Integer::sum);
	}

	/** Event key for using an item, matching task keys like "use:flint-and-steel". */
	public static String useEvent(String itemId) {
		return "use:" + itemId.replace('_', '-');
	}

	public static int count(UUID playerId, String event) {
		return COUNTS.getOrDefault(playerId, Map.of()).getOrDefault(event, 0);
	}
}
