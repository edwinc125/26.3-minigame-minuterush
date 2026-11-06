package com.edwinc125.minuterush.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TaskRequirementsTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void basicCatalogIdsMapToTheirSubject() {
		assertEquals("jungle-log", TaskRequirements.subject("basic:jungle-log:mine"));
		assertEquals("red-sand", TaskRequirements.subject("basic:red-sand:collect"));
		assertEquals("hit-villager", TaskRequirements.subject("hit-villager"));
	}

	@Test
	void villageTasksNeedAVillage() {
		for (String id : new String[] {"hit-villager", "kill-villager", "ring-bell", "feed-cat"}) {
			TaskRequirements.Requirement requirement = TaskRequirements.of(id);
			assertNotNull(requirement, id);
			assertTrue(requirement.isMet(biome -> false, true), id);
			assertFalse(requirement.isMet(biome -> true, false), id + " must not be met by any biome");
		}
	}

	@Test
	void biomeTasksNeedTheirBiome() {
		for (String id : new String[] {"basic:jungle-log:mine", "basic:acacia-log:collect", "basic:red-sand:place",
				"basic:cactus:hold", "basic:snow-block:mine", "sweet-berry-damage", "hit-horse"}) {
			TaskRequirements.Requirement requirement = TaskRequirements.of(id);
			assertNotNull(requirement, id);
			assertFalse(requirement.isMet(biome -> false, true), id + " must not be met by a village alone");
			assertTrue(requirement.isMet(biome -> true, false), id);
		}
	}

	@Test
	void emeraldsAcceptAVillageOrMountains() {
		TaskRequirements.Requirement emerald = TaskRequirements.of("basic:emerald:collect");
		assertTrue(emerald.isMet(biome -> false, true), "village alone");
		assertTrue(emerald.isMet(biome -> true, false), "mountains/hills alone");
		assertFalse(emerald.isMet(biome -> false, false), "neither");
	}

	@Test
	void bedsNeedTheirDyeBiome() {
		for (String color : new String[] {"green", "lime", "cyan", "brown", "black", "gray"}) {
			TaskRequirements.Requirement requirement = TaskRequirements.of("craft-bed-" + color);
			assertNotNull(requirement, color);
			assertFalse(requirement.isMet(biome -> false, true), color + " is not met by a village");
			assertTrue(requirement.isMet(biome -> true, false), color);
		}
		// Blue shades count as available anywhere because blue dye comes from lapis.
		for (String color : new String[] {"white", "light_gray", "red", "yellow", "orange", "pink", "blue",
				"light_blue", "purple", "magenta"}) {
			assertNull(TaskRequirements.of("craft-bed-" + color), color + " dye is available anywhere");
		}
	}

	@Test
	void requirementWithSeveralBiomesNeedsAllOfThem() {
		java.util.function.Predicate<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> a = biome -> true;
		java.util.function.Predicate<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> b = biome -> true;
		TaskRequirements.Requirement both = new TaskRequirements.Requirement(java.util.List.of(a, b), false);
		assertFalse(both.isMet(check -> check == a, false), "only one of the two biomes present");
		assertTrue(both.isMet(check -> true, false));
	}

	@Test
	void commonTasksHaveNoRequirement() {
		for (String id : new String[] {"basic:oak-log:mine", "basic:dirt:collect", "craft-stick", "hit-cow",
				"stone:stone_bricks:craft", "jump-count"}) {
			assertNull(TaskRequirements.of(id), id);
		}
	}
}
