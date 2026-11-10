package com.edwinc125.minuterush.game;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArenaHistoryTest {
	@Test
	void usedLocationsSurviveSerializationAndNewLocationsRemainEligible() {
		ArenaHistory history = new ArenaHistory();
		history.record(123L);
		history.record(-456L);
		assertTrue(history.isDirty());

		var saved = ArenaHistory.CODEC.encodeStart(JsonOps.INSTANCE, history).getOrThrow();
		ArenaHistory restored = ArenaHistory.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
		assertTrue(restored.contains(123L));
		assertTrue(restored.contains(-456L));
		assertFalse(restored.contains(789L));
	}

	@Test
	void usedLocationsSurviveWorldDataReload(@TempDir Path directory) {
		SharedConstants.tryDetectVersion();
		var registries = HolderLookup.Provider.create(Stream.empty());
		try (var storage = new SavedDataStorage(directory, DataFixers.getDataFixer(), registries)) {
			storage.computeIfAbsent(ArenaHistory.TYPE).record(123L);
		}
		try (var storage = new SavedDataStorage(directory, DataFixers.getDataFixer(), registries)) {
			ArenaHistory restored = storage.computeIfAbsent(ArenaHistory.TYPE);
			assertTrue(restored.contains(123L));
			assertFalse(restored.contains(456L));
		}
	}
}
