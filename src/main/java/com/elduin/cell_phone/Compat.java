package com.elduin.cell_phone;

//? if >=26 {
/*import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
*///? } else {
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
//? }

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

/**
 * The things Mojang and Fabric moved between the versions this mod supports, for the shared
 * (not client-only) half of the mod. The client half has its own in client/ClientCompat.
 */
public final class Compat {

	private Compat() {
	}

	/** Fabric renamed its creative-tab events in 26. */
	public static void addToCreativeTab(ResourceKey<CreativeModeTab> tab, Item item) {
		//? if >=26 {
		/*CreativeModeTabEvents.modifyOutputEvent(tab).register(output -> output.accept(item));
		*///? } else {
		ItemGroupEvents.modifyEntriesEvent(tab).register(entries -> entries.accept(item));
		//? }
	}
}
