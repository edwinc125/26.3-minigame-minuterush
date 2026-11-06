package com.edwinc125.minuterush.task;

/**
 * Detects a main-hand/offhand swap by comparing hand contents between ticks.
 * A swap only counts when two different items trade places while the selected
 * hotbar slot stays the same, so identical hands (e.g. both empty) never match.
 */
public final class OffhandSwapProgress {
	private String previousMain;
	private String previousOff;
	private int previousSlot = -1;

	public boolean update(String main, String off, int selectedSlot) {
		boolean swapped = previousMain != null
				&& previousSlot == selectedSlot
				&& !main.equals(off)
				&& previousMain.equals(off)
				&& previousOff.equals(main);
		previousMain = main;
		previousOff = off;
		previousSlot = selectedSlot;
		return swapped;
	}
}
