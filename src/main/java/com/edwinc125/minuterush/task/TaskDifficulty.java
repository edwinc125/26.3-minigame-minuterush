package com.edwinc125.minuterush.task;

import java.util.Set;

/**
 * How hard a single task is, judged mainly by the items it needs (discussion #13):
 * <ul>
 *   <li>{@link #SIMPLE} - no items at all: movement, looking, hitting mobs, interacting ("quick fire")</li>
 *   <li>{@link #EASY} - basic materials: dirt, wood, stone, copper, common mob drops</li>
 *   <li>{@link #MEDIUM} - iron, gold, redstone, string-based tools, rarely dropped items such as apples</li>
 *   <li>{@link #HARD} - diamonds (and emeralds)</li>
 * </ul>
 *
 * <p>How often each tier comes up is set by the match's {@link MatchDifficulty} (see {@link DifficultyMix}).
 */
public enum TaskDifficulty {
	SIMPLE, EASY, MEDIUM, HARD;

	/** Returns the difficulty for a task id, or null if the id is not classified. */
	public static TaskDifficulty classify(String taskId) {
		if (taskId == null) {
			return null;
		}
		if (taskId.startsWith("basic:")) {
			return basic(TaskRequirements.subject(taskId));
		}
		if (taskId.startsWith("craft-bed-")) {
			// White wool + planks is basic; these dyes need smelting, rare drops or a specific biome.
			return BED_DYES_NEEDING_EXTRA.contains(taskId.substring("craft-bed-".length())) ? MEDIUM : EASY;
		}
		if (taskId.startsWith("craft-fence-") || taskId.startsWith("craft-sign-")
				|| taskId.startsWith("craft-pressure-plate-")) {
			return EASY;
		}
		if (taskId.startsWith("craft-iron-")) {
			return MEDIUM;
		}
		if (taskId.startsWith("craft-copper-") || taskId.startsWith("craft-wooden-")
				|| taskId.startsWith("craft-leather-")) {
			return EASY;
		}
		if (taskId.startsWith("hit-player-")) {
			return taskId.equals("hit-player-fishing-rod") ? MEDIUM : EASY;
		}
		if (SIMPLE_IDS.contains(taskId)) return SIMPLE;
		if (EASY_IDS.contains(taskId)) return EASY;
		if (MEDIUM_IDS.contains(taskId)) return MEDIUM;
		return null;
	}

	/** Like {@link #classify} but falls back to Easy for unclassified ids. */
	public static TaskDifficulty of(String taskId) {
		TaskDifficulty difficulty = classify(taskId);
		return difficulty == null ? EASY : difficulty;
	}

	private static TaskDifficulty basic(String subject) {
		return switch (subject) {
			case "diamond", "emerald" -> HARD;
			case "raw-iron", "raw-gold", "redstone-dust", "lapis-lazuli", "apple", "glow-ink-sac", "clock",
				"deepslate" -> MEDIUM;
			default -> EASY;
		};
	}

	private static final Set<String> BED_DYES_NEEDING_EXTRA = Set.of("green", "lime", "cyan", "brown", "black", "gray");

	private static final Set<String> SIMPLE_IDS = Set.of(
		// Mobs and players, bare-handed
		"hit-cow", "hit-player", "hit-pig", "hit-sheep", "hit-chicken", "hit-zombie", "hit-skeleton", "hit-creeper",
		"hit-spider", "hit-enderman", "hit-villager", "hit-horse", "critical-hit", "sprint-hit", "crouch-hit",
		"kill-hostile-mob", "kill-animal", "kill-zombie", "kill-skeleton", "kill-creeper", "kill-cow", "kill-player",
		"kill-pig", "kill-sheep", "kill-chicken", "kill-spider", "kill-villager",
		"interact-player", "interact-mob", "interact-block",
		// Damage from the world
		"fall-damage", "cactus-damage", "projectile-damage", "sweet-berry-damage",
		// Movement and environment
		"jump", "jump-count", "jump-5", "walk-25", "walk-50", "sprint-15", "sprint-25", "swim-20", "swim-40",
		"crouch-walk", "crouch-walk-10", "crouch-walk-15", "climb-8", "climb-15", "climb-anything", "travel-24",
		"travel-40", "travel-north-15", "travel-north-30", "travel-south-15", "travel-south-30", "travel-east-15",
		"travel-east-30", "travel-west-15", "travel-west-30", "reach-y-50", "reach-y-100", "reach-y-120",
		"reach-y-140", "fall-five-blocks", "fall-water", "enter-water", "exit-water", "submerge-underwater",
		"swim-underwater", "stand-powder-snow",
		// Things anyone can do with whatever is at hand
		"place-crouching", "break-crouching", "break-leaves", "drop-item", "swap-offhand", "ring-bell"
	);

	private static final Set<String> EASY_IDS = Set.of(
		// Wood and stone crafting
		"craft-stick", "craft-crafting-table", "craft-oak-planks", "craft-bowl", "craft-chest", "craft-torch",
		"craft-stone-pickaxe", "craft-furnace", "craft-lightning-rod",
		// Blocks and items made from wood, stone or common drops
		"open-door", "open-trapdoor", "break-door", "break-bed", "break-torch", "place-sign", "place-banner",
		"place-flower-pot", "toggle-campfire", "extinguish-campfire", "enter-vehicle", "exit-vehicle", "ride-boat",
		"boat-20", "climb-ladder", "climb-vines", "plant-seed", "harvest-crop", "press-button-use", "pull-lever-use",
		"use-crafting-table", "open-chest", "open-barrel", "open-furnace", "open-crafting-table", "press-button",
		"pull-lever", "step-on-pressure-plate", "hold-tool", "use-hoe", "throw-snowball", "throw-egg",
		"feed-cow", "feed-pig", "feed-sheep", "feed-chicken", "eat-rotten-flesh", "eat-spider-eye",
		// Need to dig down through stone
		"reach-y-20", "reach-y-0"
	);

	private static final Set<String> MEDIUM_IDS = Set.of(
		// Iron, gold and redstone
		"craft-shears", "craft-flint-and-steel", "craft-minecart", "milk-cow", "drink-milk",
		"use-bucket", "use-shears", "use-flint-and-steel", "use-compass", "use-map", "use-shield",
		"block-attack-shield", "light-campfire", "fire-crossbow",
		// String, slime and other uncommon drops
		"shoot-bow", "use-fishing-rod", "use-lead", "climb-scaffolding", "eat-pufferfish", "eat-poisonous-potato",
		"eat-suspicious-stew", "drink-potion", "consume-honey-bottle", "feed-cat",
		// Hard to reach or survive without gear
		"fire-damage", "lava-damage", "kill-enderman"
	);
}
