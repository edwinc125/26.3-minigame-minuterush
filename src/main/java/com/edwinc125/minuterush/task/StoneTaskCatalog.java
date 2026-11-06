package com.edwinc125.minuterush.task;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/**
 * Craft, place and mine tasks for building stones: stone bricks, smooth/polished stones, deepslate,
 * tuff and mud bricks, with their stairs, slabs and walls.
 *
 * <p>Base blocks already in {@link BasicTaskCatalog} (stone, andesite, diorite, granite, tuff, deepslate,
 * cobbled deepslate) only get the actions that catalog lacks. Raw andesite/diorite/granite have no
 * craft task because their recipes need nether quartz.
 */
public final class StoneTaskCatalog {
	/** Reachable from surface stone with a furnace or stonecutter. */
	static final double COMMON = 0.30;
	/** Needs depth (deepslate, tuff) or extra materials (moss/vines, wheat for mud bricks). */
	static final double RARE = 0.15;

	private StoneTaskCatalog() {}

	public enum Make {
		CRAFT("Craft"), SMELT("Smelt");

		private final String verb;

		Make(String verb) {
			this.verb = verb;
		}

		public String verb() {
			return verb;
		}
	}

	public record Entry(Item item, Make make, boolean craft, boolean mine, boolean place, double rarityWeight) {
		public Block block() {
			return Block.byItem(item);
		}
	}

	public static List<Entry> entries() {
		List<Entry> entries = new ArrayList<>();

		// Stone and smooth stone
		entries.add(new Entry(Items.STONE, Make.SMELT, true, false, true, COMMON));
		all(entries, COMMON, Items.STONE_SLAB, Items.STONE_STAIRS);
		smelted(entries, COMMON, Items.SMOOTH_STONE);
		all(entries, COMMON, Items.SMOOTH_STONE_SLAB);

		// Stone bricks
		all(entries, COMMON, Items.STONE_BRICKS, Items.STONE_BRICK_SLAB, Items.STONE_BRICK_STAIRS,
			Items.STONE_BRICK_WALL);
		all(entries, RARE, Items.MOSSY_STONE_BRICKS, Items.MOSSY_STONE_BRICK_SLAB,
			Items.MOSSY_STONE_BRICK_STAIRS, Items.MOSSY_STONE_BRICK_WALL, Items.CHISELED_STONE_BRICKS);
		smelted(entries, RARE, Items.CRACKED_STONE_BRICKS);

		// Granite, diorite, andesite and their polished forms
		all(entries, COMMON,
			Items.GRANITE_SLAB, Items.GRANITE_STAIRS, Items.GRANITE_WALL,
			Items.POLISHED_GRANITE, Items.POLISHED_GRANITE_SLAB, Items.POLISHED_GRANITE_STAIRS,
			Items.DIORITE_SLAB, Items.DIORITE_STAIRS, Items.DIORITE_WALL,
			Items.POLISHED_DIORITE, Items.POLISHED_DIORITE_SLAB, Items.POLISHED_DIORITE_STAIRS,
			Items.ANDESITE_SLAB, Items.ANDESITE_STAIRS, Items.ANDESITE_WALL,
			Items.POLISHED_ANDESITE, Items.POLISHED_ANDESITE_SLAB, Items.POLISHED_ANDESITE_STAIRS);

		// Deepslate
		entries.add(new Entry(Items.DEEPSLATE, Make.SMELT, true, false, true, RARE));
		all(entries, RARE,
			Items.COBBLED_DEEPSLATE_SLAB, Items.COBBLED_DEEPSLATE_STAIRS, Items.COBBLED_DEEPSLATE_WALL,
			Items.POLISHED_DEEPSLATE, Items.POLISHED_DEEPSLATE_SLAB, Items.POLISHED_DEEPSLATE_STAIRS,
			Items.POLISHED_DEEPSLATE_WALL,
			Items.DEEPSLATE_BRICKS, Items.DEEPSLATE_BRICK_SLAB, Items.DEEPSLATE_BRICK_STAIRS,
			Items.DEEPSLATE_BRICK_WALL,
			Items.DEEPSLATE_TILES, Items.DEEPSLATE_TILE_SLAB, Items.DEEPSLATE_TILE_STAIRS,
			Items.DEEPSLATE_TILE_WALL,
			Items.CHISELED_DEEPSLATE);
		smelted(entries, RARE, Items.CRACKED_DEEPSLATE_BRICKS, Items.CRACKED_DEEPSLATE_TILES);

		// Tuff
		all(entries, RARE,
			Items.TUFF_SLAB, Items.TUFF_STAIRS, Items.TUFF_WALL,
			Items.POLISHED_TUFF, Items.POLISHED_TUFF_SLAB, Items.POLISHED_TUFF_STAIRS, Items.POLISHED_TUFF_WALL,
			Items.TUFF_BRICKS, Items.TUFF_BRICK_SLAB, Items.TUFF_BRICK_STAIRS, Items.TUFF_BRICK_WALL,
			Items.CHISELED_TUFF, Items.CHISELED_TUFF_BRICKS);

		// Mud bricks
		all(entries, RARE, Items.MUD_BRICKS, Items.MUD_BRICK_SLAB, Items.MUD_BRICK_STAIRS, Items.MUD_BRICK_WALL);

		return List.copyOf(entries);
	}

	private static void all(List<Entry> entries, double weight, Item... items) {
		for (Item item : items) {
			entries.add(new Entry(item, Make.CRAFT, true, true, true, weight));
		}
	}

	private static void smelted(List<Entry> entries, double weight, Item... items) {
		for (Item item : items) {
			entries.add(new Entry(item, Make.SMELT, true, true, true, weight));
		}
	}
}
