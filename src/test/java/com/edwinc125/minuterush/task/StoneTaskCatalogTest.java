package com.edwinc125.minuterush.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class StoneTaskCatalogTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void everyEntryIsAPlaceableBlockItem() {
		for (StoneTaskCatalog.Entry entry : StoneTaskCatalog.entries()) {
			assertTrue(entry.item() instanceof BlockItem, entry.item() + " is not a block item");
			assertNotEquals(Blocks.AIR, entry.block(), entry.item() + " has no block");
		}
	}

	@Test
	void noItemIsListedTwice() {
		Set<Object> seen = new HashSet<>();
		for (StoneTaskCatalog.Entry entry : StoneTaskCatalog.entries()) {
			assertTrue(seen.add(entry.item()), entry.item() + " is listed twice");
		}
	}

	@Test
	void doesNotDuplicateBasicCatalogMineOrPlaceTasks() {
		Set<Block> basicMined = new HashSet<>();
		Set<Block> basicPlaced = new HashSet<>();
		for (BasicTaskCatalog.Entry entry : BasicTaskCatalog.entries()) {
			if (entry.mine() && entry.block() != null) {
				basicMined.add(entry.block());
			}
			if (entry.place() && entry.placeBlock() != null) {
				basicPlaced.add(entry.placeBlock());
			}
		}
		for (StoneTaskCatalog.Entry entry : StoneTaskCatalog.entries()) {
			assertFalse(entry.mine() && basicMined.contains(entry.block()), entry.item() + " mine task duplicated");
			assertFalse(entry.place() && basicPlaced.contains(entry.block()), entry.item() + " place task duplicated");
		}
	}

	@Test
	void furnaceOutputsAreSmeltTasks() {
		List<?> smelted = StoneTaskCatalog.entries().stream()
			.filter(entry -> entry.make() == StoneTaskCatalog.Make.SMELT)
			.map(StoneTaskCatalog.Entry::item)
			.toList();
		assertEquals(List.of(Items.STONE, Items.SMOOTH_STONE, Items.CRACKED_STONE_BRICKS, Items.DEEPSLATE,
			Items.CRACKED_DEEPSLATE_BRICKS, Items.CRACKED_DEEPSLATE_TILES), smelted);
	}
}
