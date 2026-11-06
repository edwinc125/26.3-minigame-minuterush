package com.edwinc125.minuterush.task;

import java.util.Random;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.stats.Stats;

public final class TaskFactory {
	private TaskFactory() {}

	public static MinuteRushTask next(ServerLevel level, Random random) {
		return next(level, random, Set.of()).task();
	}

	public static Selection next(ServerLevel level, Random random, Set<String> recent) {
		return next(level, random, recent, true);
	}

	public static Selection next(ServerLevel level, Random random, Set<String> recent, boolean allowPlayerInteraction) {
		List<Selection> candidates = new ArrayList<>();
		addBasicTasks(level, candidates);
		addLegacyTasks(level, candidates);
		if (!allowPlayerInteraction) {
			candidates.removeIf(selection -> selection.id().equals("interact-player"));
		}
		List<Selection> available = candidates.stream()
			.filter(selection -> !recent.contains(selection.id()))
			.toList();
		if (available.isEmpty()) {
			available = candidates;
		}
		return TaskSelection.chooseWeighted(random, available);
	}

	public static List<Selection> category(ServerLevel level, String category) {
		List<Selection> selections = new ArrayList<>();
		switch (category) {
			case "basic" -> addBasicTasks(level, selections);
			case "movement" -> movement(selections);
			case "crafting" -> {
				addLegacyTasks(level, selections);
				selections.removeIf(selection -> !"crafting".equals(selection.actionType()));
			}
			case "combat" -> {
				addLegacyTasks(level, selections);
				selections.removeIf(selection -> !isCombat(selection.id()));
			}
			case "interactive" -> {
				addLegacyTasks(level, selections);
				selections.removeIf(selection -> !"interactive".equals(selection.actionType()));
			}
			default -> {
				return List.of();
			}
		}
		return List.copyOf(selections);
	}

	private static boolean isCombat(String id) {
		return id.startsWith("hit-") || id.startsWith("kill-")
			|| id.equals("critical-hit") || id.endsWith("-damage")
			|| id.equals("block-attack-shield") || id.endsWith("-hit");
	}

	private static void addBasicTasks(ServerLevel level, List<Selection> candidates) {
		for (BasicTaskCatalog.Entry entry : BasicTaskCatalog.entries()) {
			if (entry.block() != null) {
				if (entry.mine()) {
					candidates.add(new Selection("basic:" + entry.id() + ":mine",
						new DigBlockTask(entry.block()), entry.rarityWeight(), "mine"));
				}
				if (entry.place() && entry.placeBlock() != null) {
					candidates.add(new Selection("basic:" + entry.id() + ":place",
						new PlaceBlockTask(entry.placeBlock()), entry.rarityWeight(), "place"));
				}
			}
			if (entry.collect()) {
				candidates.add(new Selection("basic:" + entry.id() + ":collect",
					new CollectItemTask(entry.item(), 2), entry.rarityWeight(), "collect"));
			}
			if (entry.hold()) {
				candidates.add(new Selection("basic:" + entry.id() + ":hold",
					new HoldItemTask("Hold a " + entry.id().replace('-', ' '),
						stack -> stack.is(entry.item())), entry.rarityWeight(), "hold"));
			}
		}
		for (StoneTaskCatalog.Entry entry : StoneTaskCatalog.entries()) {
			String id = stoneTaskId(entry);
			if (entry.mine()) {
				candidates.add(new Selection(id + ":mine", new DigBlockTask(entry.block()), entry.rarityWeight(), "mine"));
			}
			if (entry.place()) {
				candidates.add(new Selection(id + ":place", new PlaceBlockTask(entry.block()), entry.rarityWeight(), "place"));
			}
		}
	}

	private static void addStoneCraftTasks(List<Selection> candidates) {
		for (StoneTaskCatalog.Entry entry : StoneTaskCatalog.entries()) {
			if (entry.craft()) {
				candidates.add(new Selection(stoneTaskId(entry) + ":craft",
					new CraftItemTask(entry.item(), entry.make().verb()), entry.rarityWeight(), "crafting"));
			}
		}
	}

	private static String stoneTaskId(StoneTaskCatalog.Entry entry) {
		return "stone:" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(entry.item()).getPath();
	}

	private static void addLegacyTasks(ServerLevel level, List<Selection> candidates) {
		craft(candidates, "craft-stick", Items.STICK, 1.25);
		craft(candidates, "craft-crafting-table", Items.CRAFTING_TABLE, 1.25);
		craft(candidates, "craft-oak-planks", Items.OAK_PLANKS, 1.25);
		craft(candidates, "craft-wooden-pickaxe", Items.WOODEN_PICKAXE, 1.25);
		craft(candidates, "craft-wooden-axe", Items.WOODEN_AXE, 1.25);
		craft(candidates, "craft-wooden-shovel", Items.WOODEN_SHOVEL, 1.25);
		craft(candidates, "craft-wooden-hoe", Items.WOODEN_HOE, 1.0);
		craft(candidates, "craft-wooden-sword", Items.WOODEN_SWORD, 1.0);
		craft(candidates, "craft-wooden-button", Items.OAK_BUTTON, 1.0);
		craft(candidates, "craft-wooden-pressure-plate", Items.OAK_PRESSURE_PLATE, 1.0);
		craft(candidates, "craft-bowl", Items.BOWL, 1.0);
		craft(candidates, "craft-chest", Items.CHEST, 1.0);
		craft(candidates, "craft-torch", Items.TORCH, 1.25);
		craft(candidates, "craft-stone-pickaxe", Items.STONE_PICKAXE, 1.0);
		craft(candidates, "craft-furnace", Items.FURNACE, 1.0);
		craft(candidates, "craft-leather-helmet", Items.LEATHER_HELMET, 1.0);
		craft(candidates, "craft-leather-chestplate", Items.LEATHER_CHESTPLATE, 1.0);
		craft(candidates, "craft-leather-leggings", Items.LEATHER_LEGGINGS, 1.0);
		craft(candidates, "craft-leather-boots", Items.LEATHER_BOOTS, 1.0);
		craft(candidates, "craft-iron-helmet", Items.IRON_HELMET, 0.75);
		craft(candidates, "craft-iron-chestplate", Items.IRON_CHESTPLATE, 0.75);
		craft(candidates, "craft-iron-leggings", Items.IRON_LEGGINGS, 0.75);
		craft(candidates, "craft-iron-boots", Items.IRON_BOOTS, 0.75);
		craft(candidates, "craft-iron-sword", Items.IRON_SWORD, 0.75);
		craft(candidates, "craft-iron-pickaxe", Items.IRON_PICKAXE, 0.75);
		craft(candidates, "craft-iron-axe", Items.IRON_AXE, 0.75);
		craft(candidates, "craft-iron-shovel", Items.IRON_SHOVEL, 0.75);
		craft(candidates, "craft-iron-hoe", Items.IRON_HOE, 0.75);
		craft(candidates, "craft-iron-bucket", Items.BUCKET, 0.75);
		craft(candidates, "craft-shears", Items.SHEARS, 0.75);
		craft(candidates, "craft-flint-and-steel", Items.FLINT_AND_STEEL, 0.75);
		craft(candidates, "craft-copper-sword", Items.COPPER_SWORD, 0.75);
		craft(candidates, "craft-copper-shovel", Items.COPPER_SHOVEL, 0.75);
		craft(candidates, "craft-copper-pickaxe", Items.COPPER_PICKAXE, 0.75);
		craft(candidates, "craft-copper-axe", Items.COPPER_AXE, 0.75);
		craft(candidates, "craft-copper-hoe", Items.COPPER_HOE, 0.75);
		craft(candidates, "craft-copper-helmet", Items.COPPER_HELMET, 0.75);
		craft(candidates, "craft-copper-chestplate", Items.COPPER_CHESTPLATE, 0.75);
		craft(candidates, "craft-copper-leggings", Items.COPPER_LEGGINGS, 0.75);
		craft(candidates, "craft-copper-boots", Items.COPPER_BOOTS, 0.75);
		craft(candidates, "craft-lightning-rod", Items.LIGHTNING_ROD.asList().get(0), 0.75);
		craft(candidates, "craft-iron-door", Items.IRON_DOOR, 0.75);
		craft(candidates, "craft-iron-trapdoor", Items.IRON_TRAPDOOR, 0.75);
		craft(candidates, "craft-iron-bars", Items.IRON_BARS, 0.75);
		craft(candidates, "craft-minecart", Items.MINECART, 0.75);
		for (int i = 0; i < Items.BED.asList().size(); i++) {
			craft(candidates, "craft-bed-" + i, Items.BED.asList().get(i), 0.75);
		}
		craftWoodVariants(candidates);
		addStoneCraftTasks(candidates);
		candidates.add(new Selection("hit-cow", new EntityEventTask("hit:cow", "Hit a cow")));
		candidates.add(new Selection("hit-player", new EntityEventTask("hit:player", "Hit another player")));
		candidates.add(new Selection("hit-pig", new EntityEventTask("hit:pig", "Hit a pig")));
		candidates.add(new Selection("hit-sheep", new EntityEventTask("hit:sheep", "Hit a sheep")));
		candidates.add(new Selection("hit-chicken", new EntityEventTask("hit:chicken", "Hit a chicken")));
		candidates.add(new Selection("hit-zombie", new EntityEventTask("hit:zombie", "Hit a zombie")));
		candidates.add(new Selection("hit-skeleton", new EntityEventTask("hit:skeleton", "Hit a skeleton")));
		candidates.add(new Selection("hit-creeper", new EntityEventTask("hit:creeper", "Hit a creeper")));
		candidates.add(new Selection("critical-hit", new EntityEventTask("combat:critical", "Deal a critical hit")));
		candidates.add(new Selection("fall-damage", new EntityEventTask("damage:fall", "Take exactly 1 instance of fall damage")));
		candidates.add(new Selection("cactus-damage", new EntityEventTask("damage:cactus", "Take cactus damage")));
		candidates.add(new Selection("kill-hostile-mob", new EntityEventTask("kill:hostile", "Kill any hostile mob")));
		candidates.add(new Selection("kill-animal", new EntityEventTask("kill:animal", "Kill any animal")));
		candidates.add(new Selection("milk-cow", new EntityEventTask("use:milk-cow", "Milk a cow")));
		candidates.add(new Selection("block-attack-shield",
			new EntityEventTask("combat:shield-block", "Block an attack with a shield")));
		event(candidates, "projectile-damage", "damage:projectile", "Take projectile damage");
		event(candidates, "fire-damage", "damage:fire", "Take fire damage");
		event(candidates, "lava-damage", "damage:lava", "Take lava damage and survive");
		event(candidates, "sweet-berry-damage", "damage:sweet-berry", "Take sweet berry bush damage");
		event(candidates, "sprint-hit", "combat:sprint-hit", "Deal damage while sprinting");
		event(candidates, "crouch-hit", "combat:crouch-hit", "Deal damage while crouching");
		event(candidates, "kill-zombie", "kill:zombie", "Kill a zombie");
		event(candidates, "kill-skeleton", "kill:skeleton", "Kill a skeleton");
		event(candidates, "kill-creeper", "kill:creeper", "Kill a creeper");
		event(candidates, "kill-cow", "kill:cow", "Kill a cow");
		event(candidates, "kill-player", "kill:player", "Kill another player");
		event(candidates, "kill-pig", "kill:pig", "Kill a pig");
		event(candidates, "kill-sheep", "kill:sheep", "Kill a sheep");
		event(candidates, "kill-chicken", "kill:chicken", "Kill a chicken");
		event(candidates, "hit-spider", "hit:spider", "Hit a spider");
		event(candidates, "hit-enderman", "hit:enderman", "Hit an enderman");
		event(candidates, "hit-villager", "hit:villager", "Hit a villager");
		event(candidates, "feed-cow", "feed:cow", "Feed a cow");
		event(candidates, "feed-pig", "feed:pig", "Feed a pig");
		event(candidates, "feed-sheep", "feed:sheep", "Feed a sheep");
		event(candidates, "feed-chicken", "feed:chicken", "Feed a chicken");
		event(candidates, "feed-cat", "feed:cat", "Feed a cat");
		event(candidates, "place-crouching", "block:place:crouch", "Place a block while crouching");
		event(candidates, "break-crouching", "block:break:crouch", "Break a block while crouching");
		event(candidates, "open-door", "block:open:door", "Open a door");
		event(candidates, "open-trapdoor", "block:open:trapdoor", "Open a trapdoor");
		event(candidates, "toggle-campfire", "block:use:campfire", "Use a campfire");
		event(candidates, "enter-vehicle", "vehicle:enter", "Enter a vehicle");
		event(candidates, "exit-vehicle", "vehicle:exit", "Exit a vehicle");
		event(candidates, "climb-ladder", "climb:ladder", "Climb a ladder");
		event(candidates, "climb-scaffolding", "climb:scaffolding", "Climb scaffolding");
		event(candidates, "climb-vines", "climb:vines", "Climb vines");
		event(candidates, "drop-item", "inventory:drop", "Drop an item");
		event(candidates, "swap-offhand", "inventory:swap-offhand", "Swap an item to the offhand");
		// Item tasks count vanilla's "item used" stat, which only rises when the item actually works
		// (arrow fired, projectile thrown, block tilled/lit, map created, ...) and also counts in creative.
		// The event key covers uses that vanilla does not track as a stat (see MinuteRushMod).
		itemUse(candidates, "shoot-bow", "Shoot a bow", null, Items.BOW);
		itemUse(candidates, "fire-crossbow", "Fire a crossbow", null, Items.CROSSBOW);
		itemUse(candidates, "throw-snowball", "Throw a snowball", null, Items.SNOWBALL);
		itemUse(candidates, "throw-egg", "Throw an egg", null, Items.EGG, Items.BLUE_EGG, Items.BROWN_EGG);
		itemUse(candidates, "use-fishing-rod", "Use a fishing rod", null, Items.FISHING_ROD);
		itemUse(candidates, "use-flint-and-steel", "Use flint and steel", "use:flint-and-steel", Items.FLINT_AND_STEEL);
		itemUse(candidates, "use-shears", "Use shears", "use:shears", Items.SHEARS);
		itemUse(candidates, "use-hoe", "Use a hoe", "use:hoe", Items.WOODEN_HOE, Items.STONE_HOE,
			Items.COPPER_HOE, Items.IRON_HOE, Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE);
		itemUse(candidates, "use-bucket", "Use a bucket", "use:bucket", Items.BUCKET, Items.WATER_BUCKET,
			Items.LAVA_BUCKET);
		itemUse(candidates, "use-lead", "Use a lead", "use:lead", Items.LEAD);
		itemUse(candidates, "use-map", "Use a map", null, Items.MAP);
		itemUse(candidates, "use-compass", "Use a compass", "use:compass", Items.COMPASS);
		event(candidates, "use-shield", "use:shield", "Use a shield");
		event(candidates, "break-leaves", "block:break:leaves", "Break leaves");
		event(candidates, "break-torch", "block:break:torch", "Break a torch");
		event(candidates, "break-door", "block:break:door", "Break a door");
		event(candidates, "break-bed", "block:break:bed", "Break a bed");
		event(candidates, "place-sign", "block:place:sign", "Place a sign");
		event(candidates, "place-banner", "block:place:banner", "Place a banner");
		event(candidates, "place-flower-pot", "block:place:flower-pot", "Place a flower pot");
		event(candidates, "plant-seed", "block:place:seed", "Plant a seed");
		event(candidates, "harvest-crop", "block:break:crop", "Harvest a crop");
		event(candidates, "ring-bell", "block:use:bell", "Ring a bell");
		event(candidates, "press-button-use", "block:use:button", "Press a button");
		event(candidates, "pull-lever-use", "block:use:lever", "Pull a lever");
		event(candidates, "use-crafting-table", "block:use:crafting_table", "Use a crafting table");
		event(candidates, "hit-horse", "hit:horse", "Hit a horse");
		event(candidates, "kill-spider", "kill:spider", "Kill a spider");
		event(candidates, "kill-enderman", "kill:enderman", "Kill an enderman");
		event(candidates, "kill-villager", "kill:villager", "Kill a villager");
		event(candidates, "light-campfire", "block:campfire:light", "Light a campfire");
		event(candidates, "extinguish-campfire", "block:campfire:extinguish", "Extinguish a campfire");
		event(candidates, "enter-water", "environment:enter-water", "Enter water");
		event(candidates, "exit-water", "environment:exit-water", "Exit water");
		event(candidates, "stand-powder-snow", "environment:powder-snow", "Stand in powder snow");
		stat(candidates, "submerge-underwater", "Submerge underwater", Stats.CUSTOM, Stats.WALK_UNDER_WATER_ONE_CM, 1);
		stat(candidates, "swim-underwater", "Swim underwater", Stats.CUSTOM,
			Stats.WALK_UNDER_WATER_ONE_CM, blocks(1));
		stat(candidates, "crouch-walk", "Crouch-walk for 10 blocks", Stats.CUSTOM, Stats.CROUCH_ONE_CM, 1000);
		stat(candidates, "fall-five-blocks", "Fall at least five blocks", Stats.CUSTOM, Stats.FALL_ONE_CM, 500);
		stat(candidates, "ride-boat", "Ride a boat", Stats.CUSTOM, Stats.BOAT_ONE_CM, 1);
		stat(candidates, "climb-anything", "Climb 5 blocks", Stats.CUSTOM, Stats.CLIMB_ONE_CM, 500);
		candidates.add(new Selection("eat-rotten-flesh", new ConsumeItemTask(Items.ROTTEN_FLESH)));
		candidates.add(new Selection("eat-spider-eye", new ConsumeItemTask(Items.SPIDER_EYE)));
		candidates.add(new Selection("eat-suspicious-stew", new ConsumeItemTask(Items.SUSPICIOUS_STEW)));
		candidates.add(new Selection("eat-poisonous-potato", new ConsumeItemTask(Items.POISONOUS_POTATO)));
		candidates.add(new Selection("eat-pufferfish", new ConsumeItemTask(Items.PUFFERFISH)));
		candidates.add(new Selection("drink-milk", new ConsumeItemTask(Items.MILK_BUCKET)));
		candidates.add(new Selection("drink-potion", new ConsumeItemTask(Items.POTION, true)));
		candidates.add(new Selection("consume-honey-bottle", new ConsumeItemTask(Items.HONEY_BOTTLE)));
		candidates.add(new Selection("open-chest", new InteractionTask(InteractionTask.Kind.BLOCK, "chest", 1)));
		candidates.add(new Selection("open-barrel", new InteractionTask(InteractionTask.Kind.BLOCK, "barrel", 1)));
		candidates.add(new Selection("open-furnace", new InteractionTask(InteractionTask.Kind.BLOCK, "furnace", 1)));
		candidates.add(new Selection("open-crafting-table", new InteractionTask(InteractionTask.Kind.BLOCK, "crafting_table", 1)));
		candidates.add(new Selection("press-button", new InteractionTask(InteractionTask.Kind.BLOCK, "button", 1)));
		candidates.add(new Selection("pull-lever", new InteractionTask(InteractionTask.Kind.BLOCK, "lever", 1)));
		candidates.add(new Selection("step-on-pressure-plate",
			new InteractionTask(InteractionTask.Kind.BLOCK, "pressure_plate", 1)));
		candidates.add(new Selection("jump", new JumpTask(), 1.0, "movement"));
		candidates.add(new Selection("jump-count", new StatTask("Jump 3 times", 3,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.JUMP)), 1.0, "movement"));
		candidates.add(new Selection("hold-tool", new HoldItemTask("Hold a pickaxe", stack ->
			stack.is(Items.WOODEN_PICKAXE) || stack.is(Items.STONE_PICKAXE) || stack.is(Items.IRON_PICKAXE)
				|| stack.is(Items.GOLDEN_PICKAXE) || stack.is(Items.DIAMOND_PICKAXE)
				|| stack.is(Items.NETHERITE_PICKAXE))));
		candidates.add(new Selection("interact-player", new InteractionTask(InteractionTask.Kind.PLAYER, "player", 1), 1.0, "interactive"));
		candidates.add(new Selection("interact-mob", new InteractionTask(InteractionTask.Kind.MOB, "any-mob", 1), 1.0, "interactive"));
		candidates.add(new Selection("interact-block", new InteractionTask(InteractionTask.Kind.BLOCK, "any-block", 1), 1.0, "interactive"));
		movement(candidates);
	}

	private static void craft(List<Selection> candidates, String id, net.minecraft.world.item.Item item, double weight) {
		candidates.add(new Selection(id, new CraftItemTask(item), weight, "crafting"));
	}

	private static void craftWoodVariants(List<Selection> candidates) {
		net.minecraft.world.item.Item[] fences = {
			Items.OAK_FENCE, Items.SPRUCE_FENCE, Items.BIRCH_FENCE, Items.JUNGLE_FENCE,
			Items.ACACIA_FENCE, Items.DARK_OAK_FENCE, Items.MANGROVE_FENCE, Items.CHERRY_FENCE,
			Items.PALE_OAK_FENCE, Items.BAMBOO_FENCE
		};
		net.minecraft.world.item.Item[] gates = {
			Items.OAK_FENCE_GATE, Items.SPRUCE_FENCE_GATE, Items.BIRCH_FENCE_GATE,
			Items.JUNGLE_FENCE_GATE, Items.ACACIA_FENCE_GATE, Items.DARK_OAK_FENCE_GATE,
			Items.MANGROVE_FENCE_GATE, Items.CHERRY_FENCE_GATE, Items.PALE_OAK_FENCE_GATE,
			Items.BAMBOO_FENCE_GATE
		};
		net.minecraft.world.item.Item[] signs = {
			Items.OAK_SIGN, Items.SPRUCE_SIGN, Items.BIRCH_SIGN, Items.JUNGLE_SIGN,
			Items.ACACIA_SIGN, Items.DARK_OAK_SIGN, Items.MANGROVE_SIGN, Items.CHERRY_SIGN,
			Items.PALE_OAK_SIGN, Items.BAMBOO_SIGN
		};
		net.minecraft.world.item.Item[] plates = {
			Items.OAK_PRESSURE_PLATE, Items.SPRUCE_PRESSURE_PLATE, Items.BIRCH_PRESSURE_PLATE,
			Items.JUNGLE_PRESSURE_PLATE, Items.ACACIA_PRESSURE_PLATE, Items.DARK_OAK_PRESSURE_PLATE,
			Items.MANGROVE_PRESSURE_PLATE, Items.CHERRY_PRESSURE_PLATE, Items.PALE_OAK_PRESSURE_PLATE,
			Items.BAMBOO_PRESSURE_PLATE
		};
		addCraftSet(candidates, "fence", fences);
		addCraftSet(candidates, "fence-gate", gates);
		addCraftSet(candidates, "sign", signs);
		addCraftSet(candidates, "pressure-plate", plates);
	}

	private static void addCraftSet(List<Selection> candidates, String prefix,
			net.minecraft.world.item.Item[] items) {
		for (int i = 0; i < items.length; i++) {
			craft(candidates, "craft-" + prefix + "-" + i, items[i], 0.75);
		}
	}

	private static void addDirectionalTravel(List<Selection> candidates) {
		addDirectional(candidates, "north", 0, -1);
		addDirectional(candidates, "south", 0, 1);
		addDirectional(candidates, "east", 1, 0);
		addDirectional(candidates, "west", -1, 0);
	}

	private static void addDirectional(List<Selection> candidates, String direction, double x, double z) {
		candidates.add(new Selection("travel-" + direction + "-15",
			new TravelTask(direction, x, z, 15), 1.0, "movement"));
		candidates.add(new Selection("travel-" + direction + "-30",
			new TravelTask(direction, x, z, 30), 1.0, "movement"));
	}

	private static int blocks(int distance) {
		return distance * 100;
	}

	private static void movement(List<Selection> candidates) {
		candidates.add(new Selection("walk-25", new StatTask("Walk 25 blocks", 2500,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.WALK_ONE_CM)), 1.0, "movement"));
		candidates.add(new Selection("walk-50", new StatTask("Walk 50 blocks", 5000,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.WALK_ONE_CM)), 1.0, "movement"));
		candidates.add(new Selection("sprint-15", new SprintTask(15), 1.0, "movement"));
		candidates.add(new Selection("sprint-25", new SprintTask(25), 1.0, "movement"));
		candidates.add(new Selection("swim-20", new StatTask("Swim 20 blocks", blocks(20),
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.SWIM_ONE_CM)), 1.0, "movement"));
		candidates.add(new Selection("swim-40", new StatTask("Swim 40 blocks", blocks(40),
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.SWIM_ONE_CM)), 1.0, "movement"));
		candidates.add(new Selection("crouch-walk-10", new StatTask("Crouch-walk 10 blocks", 1000,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.CROUCH_ONE_CM)), 1.0, "movement"));
		candidates.add(new Selection("crouch-walk-15", new StatTask("Crouch-walk 15 blocks", 1500,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.CROUCH_ONE_CM)), 1.0, "movement"));
		candidates.add(new Selection("climb-8", new StatTask("Climb 8 blocks", 800,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.CLIMB_ONE_CM)), 1.0, "movement"));
		candidates.add(new Selection("climb-15", new StatTask("Climb 15 blocks", 1500,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.CLIMB_ONE_CM)), 1.0, "movement"));
		candidates.add(new Selection("travel-24", new TravelTask(24), 1.0, "movement"));
		candidates.add(new Selection("travel-40", new TravelTask(40), 1.0, "movement"));
		addDirectionalTravel(candidates);
		candidates.add(new Selection("boat-20", new StatTask("Ride a boat for 20 blocks", 2000,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.BOAT_ONE_CM)), 1.0, "movement"));
		candidates.add(new Selection("jump-5", new StatTask("Jump 5 times", 5,
			player -> player.getStats().getValue(Stats.CUSTOM, Stats.JUMP)), 1.0, "movement"));
		candidates.add(new Selection("reach-y-100", new YLevelTask(true, 100), 0.75, "movement"));
		candidates.add(new Selection("reach-y-50", new YLevelTask(false, 50), 0.75, "movement"));
		candidates.add(new Selection("reach-y-120", new YLevelTask(true, 120), 0.5, "movement"));
		candidates.add(new Selection("reach-y-140", new YLevelTask(true, 140), 0.25, "movement"));
		candidates.add(new Selection("reach-y-20", new YLevelTask(false, 20), 0.5, "movement"));
		candidates.add(new Selection("reach-y-0", new YLevelTask(false, 0), 0.25, "movement"));
		candidates.add(new Selection("fall-water", new WaterLandingTask(), 0.75, "movement"));
	}

	private static void event(List<Selection> candidates, String id, String key, String prompt) {
		candidates.add(new Selection(id, new EntityEventTask(key, prompt)));
	}

	private static void itemUse(List<Selection> candidates, String id, String prompt, String eventKey, Item... items) {
		List<Item> tracked = List.of(items);
		candidates.add(new Selection(id, new StatTask(prompt, 1, player -> itemUseCount(player, eventKey, tracked))));
	}

	static int itemUseCount(ServerPlayer player, String eventKey, List<Item> items) {
		int total = eventKey == null ? 0 : EntityTaskProgress.count(player.getUUID(), eventKey);
		for (Item item : items) {
			total += player.getStats().getValue(Stats.ITEM_USED, item);
		}
		return total;
	}

	private static void stat(List<Selection> candidates, String id, String prompt,
			net.minecraft.stats.StatType<net.minecraft.resources.Identifier> type,
			net.minecraft.resources.Identifier stat, int required) {
		candidates.add(new Selection(id, new StatTask(prompt, required,
			player -> player.getStats().getValue(type, stat)), 1.0, "movement"));
	}

	public record Selection(String id, MinuteRushTask task, double rarityWeight, String actionType)
		implements TaskSelection.Weighted {
		public Selection(String id, MinuteRushTask task) {
			this(id, task, 1.0, "interactive");
		}

		@Override
		public double weight() {
			return rarityWeight * switch (actionType) {
				case "crafting" -> 1.50;
				case "movement" -> 1.40;
				case "interactive" -> 1.25;
				case "mine", "place", "collect" -> 0.80;
				case "hold" -> 0.40;
				default -> 1.0;
			};
		}
	}
}
