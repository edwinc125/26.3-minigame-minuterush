package com.edwinc125.minuterush.game;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * What exists around the arena centre, by distance. Distances are Chebyshev (max of |dx|, |dz|) because the
 * world border is a square: something at distance d is inside a border of half-size d or more.
 *
 * @param <B> biome type (generic so the distance logic can be tested without Minecraft's registries)
 */
public final class ArenaSurvey<B> {
	/** No village was found within the surveyed area. */
	public static final int NONE = Integer.MAX_VALUE;

	// Written only by the background scan before markBiomesScanned(); read only after biomesScanned is seen true.
	private final Map<B, Integer> nearestBiome = new HashMap<>();
	private volatile boolean biomesScanned;
	private volatile int villageDistance = NONE;
	private volatile boolean villageSearched;
	private final Map<String, Integer> structures = new HashMap<>();
	private volatile boolean structuresScanned;

	/** Records that {@code biome} occurs at offset (dx, dz) from the centre. Background scan only. */
	public void addBiomeSample(B biome, int dx, int dz) {
		nearestBiome.merge(biome, distance(dx, dz), Math::min);
	}

	/** Publishes the biome samples; until then no biome counts as present. */
	public void markBiomesScanned() {
		biomesScanned = true;
	}

	public boolean biomesScanned() {
		return biomesScanned;
	}

	public int biomeDiversity() {
		return biomesScanned ? nearestBiome.size() : 0;
	}

	public void setVillageOffset(int dx, int dz) {
		villageDistance = distance(dx, dz);
		villageSearched = true;
	}

	public void markNoVillage() {
		villageDistance = NONE;
		villageSearched = true;
	}

	/** False until the scan has finished, so biome tasks are not offered on a guess. */
	public boolean hasBiomeWithin(Predicate<B> biome, int halfSize) {
		if (!biomesScanned) {
			return false;
		}
		for (Map.Entry<B, Integer> entry : nearestBiome.entrySet()) {
			if (entry.getValue() <= halfSize && biome.test(entry.getKey())) {
				return true;
			}
		}
		return false;
	}

	/** False until the village search has run, so village tasks are not offered on a guess. */
	public boolean hasVillageWithin(int halfSize) {
		return villageSearched && villageDistance <= halfSize;
	}

	void recordStructure(String id, int dx, int dz) {
		structures.merge(id, distance(dx, dz), Math::min);
	}

	void markStructuresScanned() {
		structuresScanned = true;
	}

	boolean hasStructureWithin(String id, int halfSize) {
		return structuresScanned && structures.getOrDefault(id, NONE) <= halfSize;
	}

	static int distance(int dx, int dz) {
		return Math.max(Math.abs(dx), Math.abs(dz));
	}
}
