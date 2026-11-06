package com.edwinc125.minuterush.task;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * What must exist inside the world border for a task to be possible: a biome (e.g. jungle for jungle logs) and/or
 * a village (e.g. villagers, bells). Tasks without an entry are always possible.
 */
public final class TaskRequirements {
	private TaskRequirements() {}

	/**
	 * @param biomes  biome checks that must ALL be present (e.g. cyan dye needs a blue and a green source);
	 *                empty if no biome satisfies the task on its own
	 * @param village whether a village alone satisfies the task
	 */
	public record Requirement(List<Predicate<Holder<Biome>>> biomes, boolean village) {
		/** Met if a village is present and accepted, or every required biome check finds a biome. */
		public boolean isMet(Predicate<Predicate<Holder<Biome>>> biomeWithin, boolean villageWithin) {
			return (village && villageWithin) || (!biomes.isEmpty() && biomes.stream().allMatch(biomeWithin));
		}
	}

	private static final Requirement VILLAGE = new Requirement(List.of(), true);

	// Dye sources. White wool comes from sheep anywhere; a coloured bed needs its dye (on the wool or the bed).
	private static final Predicate<Holder<Biome>> CACTUS = tag(BiomeTags.IS_BADLANDS).or(keys(Biomes.DESERT));
	private static final Predicate<Holder<Biome>> SQUID = tag(BiomeTags.IS_RIVER).or(tag(BiomeTags.IS_OCEAN));

	/** Returns the requirement for a task id, or null if the task is possible anywhere. */
	public static Requirement of(String taskId) {
		return switch (subject(taskId)) {
			case "jungle-log", "jungle-planks", "bamboo" -> biome(tag(BiomeTags.IS_JUNGLE));
			case "acacia-log", "acacia-planks" -> biome(tag(BiomeTags.IS_SAVANNA));
			case "dark-oak-log", "dark-oak-planks" -> biome(keys(Biomes.DARK_FOREST));
			case "mangrove-log", "mud" -> biome(keys(Biomes.MANGROVE_SWAMP));
			case "cherry-log" -> biome(keys(Biomes.CHERRY_GROVE));
			case "spruce-log", "spruce-planks" ->
				biome(tag(BiomeTags.IS_TAIGA).or(keys(Biomes.GROVE, Biomes.WINDSWEPT_FOREST)));
			case "sweet-berry-damage" -> biome(tag(BiomeTags.IS_TAIGA));
			case "birch-log", "birch-planks" -> biome(keys(Biomes.FOREST, Biomes.FLOWER_FOREST,
				Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST));
			case "red-sand" -> biome(tag(BiomeTags.IS_BADLANDS));
			case "cactus" -> biome(tag(BiomeTags.IS_BADLANDS).or(keys(Biomes.DESERT)));
			case "snow-block" -> biome(keys(Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES, Biomes.SNOWY_TAIGA,
				Biomes.GROVE, Biomes.SNOWY_SLOPES, Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS,
				Biomes.SNOWY_BEACH, Biomes.FROZEN_RIVER));
			case "blue-orchid" -> biome(keys(Biomes.SWAMP));
			case "sunflower" -> biome(keys(Biomes.SUNFLOWER_PLAINS));
			case "hit-horse" -> biome(tag(BiomeTags.IS_SAVANNA).or(keys(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS)));
			case "hit-villager", "kill-villager", "ring-bell", "feed-cat" -> VILLAGE;
			// Emeralds come from villager trades or from ore in mountains/hills.
			case "emerald" -> new Requirement(List.of(tag(BiomeTags.IS_MOUNTAIN).or(tag(BiomeTags.IS_HILL))), true);
			// Beds by dye. White, light gray (bluet/daisy/tulip), red (poppy), yellow (dandelion), orange
			// (red + yellow) and pink (red + bone meal) dyes are available almost anywhere, so need nothing.
			// Blue dye also comes from lapis, which is underground in every biome and which players may already
			// carry from earlier rounds, so blue, light blue (blue + white), purple (red + blue) and magenta
			// (purple + pink) need nothing either.
			case "craft-bed-green" -> biome(CACTUS); // smelted cactus
			case "craft-bed-lime" -> biome(CACTUS.or(keys(Biomes.WARM_OCEAN))); // green + white, or sea pickle
			case "craft-bed-cyan" -> biome(CACTUS); // blue (lapis, anywhere) + green (cactus)
			case "craft-bed-brown" -> biome(tag(BiomeTags.IS_JUNGLE)); // cocoa beans
			case "craft-bed-black", "craft-bed-gray" -> biome(SQUID); // ink sac; gray = black + white
			default -> null;
		};
	}

	/** "basic:jungle-log:mine" -> "jungle-log"; other ids are returned unchanged. */
	static String subject(String taskId) {
		if (taskId != null && taskId.startsWith("basic:")) {
			int end = taskId.indexOf(':', "basic:".length());
			return end < 0 ? taskId.substring("basic:".length()) : taskId.substring("basic:".length(), end);
		}
		return taskId == null ? "" : taskId;
	}

	private static Requirement biome(Predicate<Holder<Biome>> predicate) {
		return new Requirement(List.of(predicate), false);
	}

	private static Predicate<Holder<Biome>> tag(TagKey<Biome> tag) {
		return holder -> holder.is(tag);
	}

	@SafeVarargs
	private static Predicate<Holder<Biome>> keys(ResourceKey<Biome>... keys) {
		return holder -> {
			for (ResourceKey<Biome> key : keys) {
				if (holder.is(key)) {
					return true;
				}
			}
			return false;
		};
	}
}
