package com.elduin.cell_phone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Client-side things that moved between the Minecraft versions this mod supports. */
final class ClientCompat {

	private ClientCompat() {
	}

	/** The screen that is open right now, or null. 26 moved screens onto the Gui. */
	static Screen screen(Minecraft mc) {
		//? if >=26 {
		/*return mc.gui.screen();
		*///? } else {
		return mc.screen;
		//? }
	}

	static void setScreen(Minecraft mc, Screen screen) {
		//? if >=26 {
		/*mc.gui.setScreen(screen);
		*///? } else {
		mc.setScreen(screen);
		//? }
	}

	/** The line just above the hotbar. */
	static void actionBar(Minecraft mc, Component text) {
		if (mc.player == null) {
			return;
		}
		//? if >=26 {
		/*mc.player.sendOverlayMessage(text);
		*///? } else {
		mc.player.displayClientMessage(text, true);
		//? }
	}

	/** Time of day, 0 to 23999, where 0 is six in the morning. */
	static long dayTime(Minecraft mc) {
		if (mc.level == null) {
			return 0;
		}
		//? if >=26 {
		/*return mc.level.getOverworldClockTime() % 24000L;
		*///? } else {
		return mc.level.getDayTime() % 24000L;
		//? }
	}
}
