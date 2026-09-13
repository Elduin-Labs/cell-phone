package com.elduin.cell_phone.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

/** What a caller does for a living, which decides what they phone you about. */
enum Job {
	FARMER(VillagerProfession.FARMER, "farmer", 0xFFB5873A),
	LIBRARIAN(VillagerProfession.LIBRARIAN, "librarian", 0xFF7A4E2D),
	ARMORER(VillagerProfession.ARMORER, "armorer", 0xFF4D4D55),
	BUTCHER(VillagerProfession.BUTCHER, "butcher", 0xFFB23A3A),
	CARTOGRAPHER(VillagerProfession.CARTOGRAPHER, "cartographer", 0xFF3A6FB2),
	CLERIC(VillagerProfession.CLERIC, "cleric", 0xFF7A3AA8),
	FISHERMAN(VillagerProfession.FISHERMAN, "fisherman", 0xFF2F8F8F),
	FLETCHER(VillagerProfession.FLETCHER, "fletcher", 0xFF5E8C3A),
	LEATHERWORKER(VillagerProfession.LEATHERWORKER, "leatherworker", 0xFF8C5A2E),
	MASON(VillagerProfession.MASON, "mason", 0xFF7D7D7D),
	SHEPHERD(VillagerProfession.SHEPHERD, "shepherd", 0xFFD9D9D9),
	TOOLSMITH(VillagerProfession.TOOLSMITH, "toolsmith", 0xFF3D3D3D),
	WEAPONSMITH(VillagerProfession.WEAPONSMITH, "weaponsmith", 0xFF2B2B2B),
	NITWIT(VillagerProfession.NITWIT, "nitwit", 0xFF3E8E3E),
	UNEMPLOYED(VillagerProfession.NONE, "none", 0xFF6B4A33);

	private final ResourceKey<VillagerProfession> key;
	private final String path;
	/** Background behind the caller's face on the phone. */
	final int color;

	Job(ResourceKey<VillagerProfession> key, String path, int color) {
		this.key = key;
		this.path = path;
		this.color = color;
	}

	/** "Farmer", "Librarian"... in whatever language the game is set to. */
	Component title() {
		return Component.translatable("entity.minecraft.villager." + path);
	}

	static Job of(Villager villager) {
		for (Job job : values()) {
			if (villager.getVillagerData().profession().is(job.key)) {
				return job;
			}
		}
		return UNEMPLOYED;
	}
}
