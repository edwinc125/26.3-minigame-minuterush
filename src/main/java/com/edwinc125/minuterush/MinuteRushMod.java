package com.edwinc125.minuterush;

import com.edwinc125.minuterush.command.MinuteRushCommands;
import com.edwinc125.minuterush.game.MinuteRushGame;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Items;
import com.edwinc125.minuterush.task.EntityTaskProgress;
import com.edwinc125.minuterush.task.OffhandSwapProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.material.Fluids;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.resources.Identifier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MinuteRushMod implements ModInitializer {
	public static final String MOD_ID = "minute_rush";
	private static MinuteRushGame game;
	private static final Map<UUID, Boolean> PASSENGER_STATE = new HashMap<>();
	private static final Map<UUID, OffhandSwapProgress> OFFHAND_SWAP_STATE = new HashMap<>();
	/** Checks that need to see the result of an interaction; see {@link DeferredChecks}. */
	private static final DeferredChecks AFTER_INTERACTION = new DeferredChecks();

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> game = new MinuteRushGame(server));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			game = null;
			AFTER_INTERACTION.clear();
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			// Before game.tick(), so a task completed by an interaction is scored this tick.
			AFTER_INTERACTION.runAll();
			if (game != null) {
				game.tick();
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				recordMovementEvents(player);
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (game != null) {
				game.playerJoined(handler.getPlayer());
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (game != null) {
				game.playerLeft(handler.getPlayer());
			}
		});
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (game == null || world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
				return InteractionResult.PASS;
			}
			ItemStack heldBefore = serverPlayer.getItemInHand(hand).copy();
			String block = blockId(serverPlayer, hitResult.getBlockPos());
			EntityTaskProgress.record(serverPlayer.getUUID(), "block:use:" + block);
			recordBlockUse(serverPlayer, block);
			if (isSeed(heldBefore.getItem())) {
				recordSuccessfulItemUse(serverPlayer, hand, hitResult, heldBefore);
			}
			if (isBucket(heldBefore.getItem())) {
				recordDeferredBucketUse(serverPlayer, hand, heldBefore);
			}
			if (isHoe(heldBefore.getItem())) {
				recordDeferredToolUse(serverPlayer, hand, heldBefore, "use:hoe");
			}
			if (heldBefore.is(Items.FLINT_AND_STEEL)) {
				// Lighting TNT has no vanilla stat; fires and campfires are also covered by the stat.
				recordDeferredToolUse(serverPlayer, hand, heldBefore, EntityTaskProgress.useEvent("flint_and_steel"));
			}
			if (heldBefore.is(Items.SHEARS)) {
				// Carving a pumpkin or harvesting a beehive has no vanilla stat.
				recordDeferredToolUse(serverPlayer, hand, heldBefore, "use:shears");
			}
			if (serverPlayer.isCrouching()) {
				EntityTaskProgress.record(serverPlayer.getUUID(), "block:place:crouch");
			}
			BlockState clickedState = world.getBlockState(hitResult.getBlockPos());
			if (clickedState.getBlock() instanceof net.minecraft.world.level.block.DoorBlock
					&& !clickedState.getValue(net.minecraft.world.level.block.DoorBlock.OPEN)) {
				recordDeferredOpen(serverPlayer, hitResult.getBlockPos(), clickedState, "block:open:door");
			}
			if (clickedState.getBlock() instanceof net.minecraft.world.level.block.TrapDoorBlock
					&& !clickedState.getValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN)) {
				recordDeferredOpen(serverPlayer, hitResult.getBlockPos(), clickedState, "block:open:trapdoor");
			}
			if (block.contains("campfire")) {
				EntityTaskProgress.record(serverPlayer.getUUID(), "block:use:campfire");
				BlockState state = world.getBlockState(hitResult.getBlockPos());
				if (state.hasProperty(net.minecraft.world.level.block.CampfireBlock.LIT)
						&& !state.getValue(net.minecraft.world.level.block.CampfireBlock.LIT)
						&& heldBefore.is(Items.FLINT_AND_STEEL)) {
					recordSuccessfulCampfireLighting(serverPlayer, hitResult.getBlockPos(), heldBefore, hand);
				}
				if (state.hasProperty(net.minecraft.world.level.block.CampfireBlock.LIT)
						&& state.getValue(net.minecraft.world.level.block.CampfireBlock.LIT)
						&& BuiltInRegistries.ITEM.getKey(serverPlayer.getItemInHand(hand).getItem())
							.getPath().endsWith("_shovel")) {
					EntityTaskProgress.record(serverPlayer.getUUID(), "block:campfire:extinguish");
				}
			}
			game.blockUsed(serverPlayer, hitResult.getBlockPos());
			return InteractionResult.PASS;
		});
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (game != null && !world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
				ItemStack heldBefore = serverPlayer.getItemInHand(hand).copy();
				String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
				if (isFeedItem(heldBefore.getItem(), entityId)) {
					EntityTaskProgress.record(serverPlayer.getUUID(), "feed:" + feedTarget(entityId));
				}
				if (heldBefore.is(Items.SHEARS)) {
					recordDeferredToolUse(serverPlayer, hand, heldBefore, "use:shears");
				}
				if (heldBefore.is(Items.FLINT_AND_STEEL)) {
					// Igniting a creeper.
					recordDeferredToolUse(serverPlayer, hand, heldBefore, EntityTaskProgress.useEvent("flint_and_steel"));
				}
				if (heldBefore.is(Items.LEAD) && entity instanceof Leashable leashable
						&& leashable.getLeashHolder() != serverPlayer) {
					// Leashing a mob has no vanilla stat (tying to a fence does).
					recordSuccessfulLeash(serverPlayer, leashable);
				}
				if ("cow".equals(entityId)
						&& heldBefore.is(Items.BUCKET)) {
					recordSuccessfulMilking(serverPlayer, hand, entity, "use:milk-cow");
				}
				game.entityUsed(serverPlayer, entity instanceof ServerPlayer ? "player" : entityId);
			}
			return InteractionResult.PASS;
		});
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (game != null && !world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
				String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
				EntityTaskProgress.record(serverPlayer.getUUID(), "hit:" + entityId);
				if (serverPlayer.isSprinting()) {
					EntityTaskProgress.record(serverPlayer.getUUID(), "combat:sprint-hit");
				}
				if (serverPlayer.isCrouching()) {
					EntityTaskProgress.record(serverPlayer.getUUID(), "combat:crouch-hit");
				}
				if (!serverPlayer.onGround() && serverPlayer.fallDistance > 0.0f
						&& !serverPlayer.isSprinting() && !serverPlayer.isPassenger()) {
					EntityTaskProgress.record(serverPlayer.getUUID(), "combat:critical");
				}
				game.entityUsed(serverPlayer, entity instanceof ServerPlayer ? "player" : entityId);
			}
			return InteractionResult.PASS;
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (entity instanceof ServerPlayer player) {
				if (source.is(DamageTypeTags.IS_FALL)) {
					EntityTaskProgress.record(player.getUUID(), "damage:fall");
				}
				if ("cactus".equals(source.getMsgId())) {
					EntityTaskProgress.record(player.getUUID(), "damage:cactus");
				}
				if (source.is(DamageTypeTags.IS_PROJECTILE)) {
					EntityTaskProgress.record(player.getUUID(), "damage:projectile");
				}
				if (DamageTypeTags.IS_FIRE != null && source.is(DamageTypeTags.IS_FIRE)) {
					EntityTaskProgress.record(player.getUUID(), "damage:fire");
				}
				if ("lava".equals(source.getMsgId())) {
					EntityTaskProgress.record(player.getUUID(), "damage:lava");
				}
				if ("sweetBerryBush".equals(source.getMsgId())) {
					EntityTaskProgress.record(player.getUUID(), "damage:sweet-berry");
				}
				if (blocked) {
					EntityTaskProgress.record(player.getUUID(), "combat:shield-block");
				}
				// Melee (including spear) damage dealt by another player, by weapon type.
				if (source.getEntity() instanceof ServerPlayer attacker && attacker != player
						&& source.getDirectEntity() == attacker && damageTaken > 0) {
					String tool = weaponType(source.getWeaponItem());
					if (tool != null) {
						EntityTaskProgress.record(attacker.getUUID(), "hit:player:" + tool);
					}
				}
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getEntity() instanceof ServerPlayer player) {
			game.playerDied(player);
			if (entity.getType().getCategory() == MobCategory.MONSTER) {
					EntityTaskProgress.record(player.getUUID(), "kill:hostile");
				}
				if (entity instanceof Animal) {
					EntityTaskProgress.record(player.getUUID(), "kill:animal");
				}
				EntityTaskProgress.record(player.getUUID(),
					"kill:" + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath());
			}
		});
		// Break tasks count only once the block is actually broken, not when the player starts hitting it.
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
				if (serverPlayer.isCrouching()) {
					EntityTaskProgress.record(serverPlayer.getUUID(), "block:break:crouch");
				}
				recordBlockBreak(serverPlayer, state);
			}
		});
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
				ItemStack heldBefore = serverPlayer.getItemInHand(hand).copy();
				Item item = heldBefore.getItem();
				if (isBucket(item)) {
					return InteractionResult.PASS;
				}
				// Bows, crossbows, snowballs, eggs, fishing rods and maps are tracked by the vanilla
				// "item used" stat (see TaskFactory#itemUse), which only rises on an actual use.
				// Raising a shield and right-clicking a compass have no stat, so record them here.
				if (item == Items.SHIELD || item == Items.COMPASS) {
					EntityTaskProgress.record(serverPlayer.getUUID(),
						EntityTaskProgress.useEvent(BuiltInRegistries.ITEM.getKey(item).getPath()));
				}
			}
			return InteractionResult.PASS;
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			MinuteRushCommands.register(dispatcher, () -> game));
	}

	private static void recordMovementEvents(ServerPlayer player) {
		boolean passenger = player.isPassenger();
		boolean previous = PASSENGER_STATE.getOrDefault(player.getUUID(), passenger);
		if (passenger && !previous) {
			EntityTaskProgress.record(player.getUUID(), "vehicle:enter");
		} else if (!passenger && previous) {
			EntityTaskProgress.record(player.getUUID(), "vehicle:exit");
		}
		PASSENGER_STATE.put(player.getUUID(), passenger);
		String block = blockId(player, player.blockPosition());
		boolean inWater = player.isInWater();
		boolean previousWater = WATER_STATE.getOrDefault(player.getUUID(), inWater);
		if (inWater && !previousWater) {
			EntityTaskProgress.record(player.getUUID(), "environment:enter-water");
		} else if (!inWater && previousWater) {
			EntityTaskProgress.record(player.getUUID(), "environment:exit-water");
		}
		WATER_STATE.put(player.getUUID(), inWater);
		if (block.contains("powder_snow")) {
			EntityTaskProgress.record(player.getUUID(), "environment:powder-snow");
		}
		if (block.contains("ladder")) {
			EntityTaskProgress.record(player.getUUID(), "climb:ladder");
		} else if (block.contains("scaffolding")) {
			EntityTaskProgress.record(player.getUUID(), "climb:scaffolding");
		} else if (block.contains("vine")) {
			EntityTaskProgress.record(player.getUUID(), "climb:vines");
		}
		int drops = player.getStats().getValue(Stats.CUSTOM, Stats.DROP);
		int previousDrops = DROP_STATE.getOrDefault(player.getUUID(), drops);
		if (drops > previousDrops) {
			EntityTaskProgress.record(player.getUUID(), "inventory:drop");
		}
		DROP_STATE.put(player.getUUID(), drops);
		String main = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).getPath();
		String off = BuiltInRegistries.ITEM.getKey(player.getOffhandItem().getItem()).getPath();
		OffhandSwapProgress swap = OFFHAND_SWAP_STATE.computeIfAbsent(player.getUUID(), id -> new OffhandSwapProgress());
		if (swap.update(main, off, player.getInventory().getSelectedSlot())) {
			EntityTaskProgress.record(player.getUUID(), "inventory:swap-offhand");
		}
	}

	private static final Map<UUID, Integer> DROP_STATE = new HashMap<>();
	private static final Map<UUID, Boolean> WATER_STATE = new HashMap<>();

	private static String blockId(ServerPlayer player, net.minecraft.core.BlockPos pos) {
		return BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(pos).getBlock()).getPath();
	}

	private static void recordBlockUse(ServerPlayer player, String block) {
		if (block.endsWith("_button")) {
			EntityTaskProgress.record(player.getUUID(), "block:use:button");
		} else if ("lever".equals(block)) {
			EntityTaskProgress.record(player.getUUID(), "block:use:lever");
		} else if ("bell".equals(block)) {
			EntityTaskProgress.record(player.getUUID(), "block:use:bell");
		} else if ("chest".equals(block) || "trapped_chest".equals(block)) {
			EntityTaskProgress.record(player.getUUID(), "block:use:chest");
		} else if ("barrel".equals(block)) {
			EntityTaskProgress.record(player.getUUID(), "block:use:barrel");
		} else if (block.endsWith("furnace")) {
			EntityTaskProgress.record(player.getUUID(), "block:use:furnace");
		} else if ("crafting_table".equals(block)) {
			EntityTaskProgress.record(player.getUUID(), "block:use:crafting_table");
		}
		String held = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).getPath();
		if (held.endsWith("_sign")) {
			EntityTaskProgress.record(player.getUUID(), "block:place:sign");
		}
		if (held.endsWith("_banner")) {
			EntityTaskProgress.record(player.getUUID(), "block:place:banner");
		}
		if ("flower_pot".equals(held)) {
			EntityTaskProgress.record(player.getUUID(), "block:place:flower-pot");
		}
	}

	/** Plantable items that are also food; planting them is detected by the crop appearing. */
	private static boolean isSeed(Item item) {
		// Item ids are "carrot"/"potato"; "carrots"/"potatoes" are the block ids and never matched.
		return item == Items.CARROT || item == Items.POTATO;
	}

	private static boolean isBucket(Item item) {
		return item == Items.BUCKET || item == Items.WATER_BUCKET || item == Items.LAVA_BUCKET;
	}

	private static void recordSuccessfulItemUse(ServerPlayer player, InteractionHand hand,
			net.minecraft.world.phys.BlockHitResult hitResult, ItemStack heldBefore) {
		BlockState clickedBefore = player.level().getBlockState(hitResult.getBlockPos());
		net.minecraft.core.BlockPos adjacentPos = hitResult.getBlockPos().relative(hitResult.getDirection());
		BlockState adjacentBefore = player.level().getBlockState(adjacentPos);
		AFTER_INTERACTION.add(() -> {
			if (game == null || !player.isAlive()) {
				return;
			}
			Item beforeItem = heldBefore.getItem();
			if (isSeed(beforeItem) && plantedCrop(player, hitResult.getBlockPos(), clickedBefore,
					adjacentPos, adjacentBefore)) {
				EntityTaskProgress.record(player.getUUID(), "block:place:seed");
			}
		});
	}

	private static void recordDeferredBucketUse(ServerPlayer player, InteractionHand hand,
			ItemStack heldBefore) {
		AFTER_INTERACTION.add(() -> {
			if (game == null || !player.isAlive()) {
				return;
			}
			Item before = heldBefore.getItem();
			Item after = player.getItemInHand(hand).getItem();
			boolean filled = before == Items.BUCKET
					&& (after == Items.WATER_BUCKET || after == Items.LAVA_BUCKET);
			boolean emptied = (before == Items.WATER_BUCKET || before == Items.LAVA_BUCKET)
					&& after == Items.BUCKET;
			if (filled || emptied) {
				EntityTaskProgress.record(player.getUUID(), "use:bucket");
			}
		});
	}

	private static void recordDeferredToolUse(ServerPlayer player, InteractionHand hand,
			ItemStack heldBefore, String event) {
		AFTER_INTERACTION.add(() -> {
			if (game == null || !player.isAlive()) {
				return;
			}
			if (toolDurabilityDecreased(heldBefore, player.getItemInHand(hand))) {
				EntityTaskProgress.record(player.getUUID(), event);
			}
		});
	}

	/**
	 * Records the event only if the clicked closed door/trapdoor is actually open after the interaction.
	 * Placing a door, clicking an iron door by hand, or a cancelled click therefore does not count.
	 */
	private static void recordDeferredOpen(ServerPlayer player, net.minecraft.core.BlockPos pos,
			BlockState before, String event) {
		AFTER_INTERACTION.add(() -> {
			BlockState after = player.level().getBlockState(pos);
			if (game != null && after.is(before.getBlock())
					&& after.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)
					&& after.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)) {
				EntityTaskProgress.record(player.getUUID(), event);
			}
		});
	}

	private static void recordSuccessfulLeash(ServerPlayer player, Leashable leashable) {
		AFTER_INTERACTION.add(() -> {
			if (game != null && leashable.getLeashHolder() == player) {
				EntityTaskProgress.record(player.getUUID(), "use:lead");
			}
		});
	}

	private static void recordSuccessfulMilking(ServerPlayer player, InteractionHand hand,
			Entity entity, String event) {
		AFTER_INTERACTION.add(() -> {
			if (game != null && entity.isAlive()
					&& player.getItemInHand(hand).is(Items.MILK_BUCKET)) {
				EntityTaskProgress.record(player.getUUID(), event);
			}
		});
	}

	private static boolean toolDurabilityDecreased(ItemStack before, ItemStack after) {
		return before.getItem() == after.getItem()
				&& after.getDamageValue() > before.getDamageValue();
	}

	private static void recordSuccessfulCampfireLighting(ServerPlayer player,
			net.minecraft.core.BlockPos pos, ItemStack heldBefore, InteractionHand hand) {
		AFTER_INTERACTION.add(() -> {
			BlockState state = player.level().getBlockState(pos);
			if (game != null && state.hasProperty(net.minecraft.world.level.block.CampfireBlock.LIT)
					&& state.getValue(net.minecraft.world.level.block.CampfireBlock.LIT)
					&& toolDurabilityDecreased(heldBefore, player.getItemInHand(hand))) {
				EntityTaskProgress.record(player.getUUID(), "block:campfire:light");
			}
		});
	}

	private static boolean plantedCrop(ServerPlayer player, net.minecraft.core.BlockPos clickedPos,
			BlockState clickedBefore, net.minecraft.core.BlockPos adjacentPos, BlockState adjacentBefore) {
		BlockState clickedAfter = player.level().getBlockState(clickedPos);
		BlockState adjacentAfter = player.level().getBlockState(adjacentPos);
		return ("farmland".equals(blockName(clickedBefore)) && isNewCrop(clickedBefore, clickedAfter))
				|| isNewCrop(adjacentBefore, adjacentAfter)
				|| isNewCrop(clickedBefore, clickedAfter);
	}

	private static boolean isNewCrop(BlockState before, BlockState after) {
		return !(before.getBlock() instanceof CropBlock) && after.getBlock() instanceof CropBlock;
	}

	private static String blockName(BlockState state) {
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
	}

	private static void recordBlockBreak(ServerPlayer player, BlockState state) {
		String block = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
		EntityTaskProgress.record(player.getUUID(), "block:break:" + block);
		if (block.endsWith("_leaves")) {
			EntityTaskProgress.record(player.getUUID(), "block:break:leaves");
		}
		if (block.endsWith("_door")) {
			EntityTaskProgress.record(player.getUUID(), "block:break:door");
		}
		if (block.endsWith("_bed")) {
			EntityTaskProgress.record(player.getUUID(), "block:break:bed");
		}
		if ("torch".equals(block) || "wall_torch".equals(block)) {
			EntityTaskProgress.record(player.getUUID(), "block:break:torch");
		}
		if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
			// Only fully grown crops count as a harvest.
			EntityTaskProgress.record(player.getUUID(), "block:break:crop");
		}
	}

	private static String weaponType(ItemStack weapon) {
		if (weapon == null || weapon.isEmpty()) {
			return null;
		}
		if (weapon.is(net.minecraft.tags.ItemTags.SWORDS)) return "sword";
		if (weapon.is(net.minecraft.tags.ItemTags.AXES)) return "axe";
		if (weapon.is(net.minecraft.tags.ItemTags.SHOVELS)) return "shovel";
		if (weapon.is(net.minecraft.tags.ItemTags.PICKAXES)) return "pickaxe";
		if (weapon.is(net.minecraft.tags.ItemTags.HOES)) return "hoe";
		if (weapon.is(net.minecraft.tags.ItemTags.SPEARS)) return "spear";
		return null;
	}

	private static boolean isHoe(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).getPath().endsWith("_hoe");
	}


	private static boolean isFeedItem(Item item, String entityId) {
		String id = BuiltInRegistries.ITEM.getKey(item).getPath();
		return switch (entityId) {
			case "cow", "sheep" -> "wheat".equals(id);
			case "pig" -> id.equals("carrot") || id.equals("potato") || id.equals("beetroot");
			case "chicken" -> id.endsWith("_seeds");
			case "cat" -> id.equals("cod") || id.equals("salmon");
			default -> false;
		};
	}

	private static String feedTarget(String entityId) {
		return switch (entityId) {
			case "cow", "sheep", "pig", "chicken", "wolf", "cat" -> entityId;
			default -> "unknown";
		};
	}

	public static void broadcast(String message) {
		if (game != null) {
			game.server().getPlayerList().broadcastSystemMessage(Component.literal(message), false);
		}
	}
}
