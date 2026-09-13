package com.elduin.cell_phone;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/** The one thing this mod adds: the phone. */
public final class PhoneItems {

	public static final Item CELL_PHONE = cellPhone();

	private PhoneItems() {
	}

	/** Villagers only call you if the phone is somewhere in your inventory. */
	public static boolean carrying(Player player) {
		return player.getInventory().contains(stack -> stack.is(CELL_PHONE));
	}

	static void register() {
		// Touching CELL_PHONE is enough to register it; this puts it in the creative menu.
		Compat.addToCreativeTab(CreativeModeTabs.TOOLS_AND_UTILITIES, CELL_PHONE);
	}

	private static Item cellPhone() {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, CellPhone.id("cell_phone"));
		return Registry.register(BuiltInRegistries.ITEM, key,
				new CellPhoneItem(new Item.Properties().setId(key).stacksTo(1)));
	}
}
