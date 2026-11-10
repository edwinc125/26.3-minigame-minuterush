package com.edwinc125.minuterush.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

final class ArenaHistory extends SavedData {
	static final Codec<ArenaHistory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.LONG.listOf().fieldOf("centers").forGetter(history -> List.copyOf(history.centers))
	).apply(instance, ArenaHistory::new));
	static final SavedDataType<ArenaHistory> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath("minuterush", "arena_history"), ArenaHistory::new, CODEC,
		DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Set<Long> centers;

	ArenaHistory() {
		this(List.of());
	}

	private ArenaHistory(List<Long> centers) {
		this.centers = new HashSet<>(centers);
	}

	boolean contains(long center) {
		return centers.contains(center);
	}

	void record(long center) {
		if (centers.add(center)) {
			setDirty();
		}
	}
}
