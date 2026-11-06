package com.edwinc125.minuterush.game;

import java.util.List;

/** Formats one or more winners for chat and titles, e.g. "Alex", "Alex and Sam", "Alex, Sam and Kai". */
public final class WinnerNames {
	private WinnerNames() {}

	public static String join(List<String> names) {
		if (names.isEmpty()) {
			return "";
		}
		if (names.size() == 1) {
			return names.get(0);
		}
		return String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.get(names.size() - 1);
	}

	/** "wins" for one winner, "win" for several. */
	public static String verb(List<String> names) {
		return names.size() == 1 ? "wins" : "win";
	}
}
