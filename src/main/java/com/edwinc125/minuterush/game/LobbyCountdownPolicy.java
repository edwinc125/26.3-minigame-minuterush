package com.edwinc125.minuterush.game;

public final class LobbyCountdownPolicy {
	public static final int INITIAL_TICKS = 120 * 20;
	public static final int MAJORITY_TICKS = 30 * 20;
	public static final int ALL_READY_TICKS = 5 * 20;
	public static final int EXTENSION_TICKS = 60 * 20;

	private LobbyCountdownPolicy() {}

	public static int requiredReady(int activePlayers) {
		return (activePlayers + 1) / 2;
	}

	public static String bossbarText(int remainingTicks) {
		int seconds = Math.max(0, (remainingTicks + 19) / 20);
		return seconds <= 5
			? "Starting in " + seconds + "s"
			: "Waiting for more players to be ready... | " + seconds + "s";
	}
}
