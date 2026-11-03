package com.edwinc125.minuterush.task;

import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class BasicTaskCatalog {
	private BasicTaskCatalog() {}

	public static List<Entry> entries() {
		return List.of(
			block("dirt", Blocks.DIRT, 1.25), block("coarse-dirt", Blocks.COARSE_DIRT, 1.0),
			block("rooted-dirt", Blocks.ROOTED_DIRT, 0.75), block("mud", Blocks.MUD, 0.75),
			block("sand", Blocks.SAND, 1.25), block("red-sand", Blocks.RED_SAND, 0.75),
			block("gravel", Blocks.GRAVEL, 1.0),
			mappedBlock("clay", Blocks.CLAY, Items.CLAY_BALL, null, 0.75),
			mappedBlock("snow-block", Blocks.SNOW_BLOCK, Items.SNOWBALL, null, 0.75),
			block("oak-log", Blocks.OAK_LOG, 1.25), block("spruce-log", Blocks.SPRUCE_LOG, 1.0),
			block("birch-log", Blocks.BIRCH_LOG, 1.0), block("jungle-log", Blocks.JUNGLE_LOG, 0.75),
			block("acacia-log", Blocks.ACACIA_LOG, 0.75), block("dark-oak-log", Blocks.DARK_OAK_LOG, 0.75),
			block("mangrove-log", Blocks.MANGROVE_LOG, 0.75), block("cherry-log", Blocks.CHERRY_LOG, 0.75),
			block("oak-planks", Blocks.OAK_PLANKS, 1.25), block("spruce-planks", Blocks.SPRUCE_PLANKS, 1.0),
			block("birch-planks", Blocks.BIRCH_PLANKS, 1.0), block("jungle-planks", Blocks.JUNGLE_PLANKS, 0.75),
			block("acacia-planks", Blocks.ACACIA_PLANKS, 0.75), block("dark-oak-planks", Blocks.DARK_OAK_PLANKS, 0.75),
			mappedBlock("stone", Blocks.STONE, Items.COBBLESTONE, Blocks.COBBLESTONE, 1.25),
			block("cobblestone", Blocks.COBBLESTONE, 1.25),
			mineOnly("deepslate", Blocks.DEEPSLATE, 0.30),
			block("cobbled-deepslate", Blocks.COBBLED_DEEPSLATE, 0.75),
			block("andesite", Blocks.ANDESITE, 0.75), block("diorite", Blocks.DIORITE, 0.75),
			block("granite", Blocks.GRANITE, 0.75), block("tuff", Blocks.TUFF, 0.75),
			item("white-wool", Items.WOOL.white(), 1.0), item("red-wool", Items.WOOL.red(), 0.75),
			item("blue-wool", Items.WOOL.blue(), 0.75), block("oak-sapling", Blocks.OAK_SAPLING, 1.0),
			block("torch", Blocks.TORCH, 1.25), block("cactus", Blocks.CACTUS, 0.75),
			vegetation("dandelion", Blocks.DANDELION, 1.0), vegetation("poppy", Blocks.POPPY, 1.0),
			vegetation("blue-orchid", Blocks.BLUE_ORCHID, 0.75), vegetation("oxeye-daisy", Blocks.OXEYE_DAISY, 0.75),
			vegetation("sunflower", Blocks.SUNFLOWER, 0.75), vegetation("sugar-cane", Blocks.SUGAR_CANE, 1.0),
			vegetation("bamboo", Blocks.BAMBOO, 0.75),
			item("apple", Items.APPLE, 1.25), item("bread", Items.BREAD, 1.0),
			item("coal", Items.COAL, 1.0), item("raw-iron", Items.RAW_IRON, 0.75),
			item("raw-copper", Items.RAW_COPPER, 0.75), item("raw-gold", Items.RAW_GOLD, 0.30),
			item("redstone-dust", Items.REDSTONE, 0.30), item("lapis-lazuli", Items.LAPIS_LAZULI, 0.30),
			item("diamond", Items.DIAMOND, 0.30), item("emerald", Items.EMERALD, 0.30),
			item("flint", Items.FLINT, 0.75), item("leather", Items.LEATHER, 0.75),
			item("string", Items.STRING, 0.75), item("bone", Items.BONE, 0.75),
			item("feather", Items.FEATHER, 0.75), item("ink-sac", Items.INK_SAC, 0.75),
			item("glow-ink-sac", Items.GLOW_INK_SAC, 0.30),
			holdOnly("clock", Items.CLOCK, 1.0)
		);
	}

	private static Entry block(String id, Block block, double weight) {
		return mappedBlock(id, block, block.asItem(), block, weight);
	}

	private static Entry mineOnly(String id, Block block, double weight) {
		return new Entry(id, block.asItem(), block, null, true, false, false, false, weight);
	}

	private static Entry vegetation(String id, Block block, double weight) {
		return new Entry(id, block.asItem(), block, block, false, true, true, true, weight);
	}

	private static Entry item(String id, Item item, double weight) {
		return new Entry(id, item, null, null, false, false, true, true, weight);
	}

	private static Entry holdOnly(String id, Item item, double weight) {
		return new Entry(id, item, null, null, false, false, false, true, weight);
	}

	private static Entry mappedBlock(String id, Block mineBlock, Item item, Block placeBlock, double weight) {
		return new Entry(id, item, mineBlock, placeBlock, true, placeBlock != null, true, true, weight);
	}

	public record Entry(
		String id,
		Item item,
		Block block,
		Block placeBlock,
		boolean mine,
		boolean place,
		boolean collect,
		boolean hold,
		double rarityWeight
	) {}
}
