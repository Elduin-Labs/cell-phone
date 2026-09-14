package com.elduin.cell_phone.client;

//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? } else {
/*import net.minecraft.client.gui.GuiGraphics;
*///? }

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Drawing on the screen. 26 renamed GuiGraphics and all of its methods, so the phone screens
 * draw through this and never touch either one directly.
 *
 * Colours are 0xAARRGGBB. Leave out the AA and the thing is invisible.
 */
final class Draw {

	//? if >=26 {
	private final GuiGraphicsExtractor g;

	Draw(GuiGraphicsExtractor g) {
		this.g = g;
	}
	//? } else {
	/*private final GuiGraphics g;

	Draw(GuiGraphics g) {
		this.g = g;
	}
	*///? }

	void fill(int x1, int y1, int x2, int y2, int color) {
		g.fill(x1, y1, x2, y2, color);
	}

	/** A rectangle with its corners nipped off, which reads as rounded at this size. */
	void round(int x1, int y1, int x2, int y2, int color) {
		g.fill(x1 + 1, y1, x2 - 1, y2, color);
		g.fill(x1, y1 + 1, x2, y2 - 1, color);
	}

	void text(Font font, Component text, int x, int y, int color, boolean shadow) {
		//? if >=26 {
		g.text(font, text, x, y, color, shadow);
		//? } else {
		/*g.drawString(font, text, x, y, color, shadow);
		*///? }
	}

	void text(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
		//? if >=26 {
		g.text(font, text, x, y, color, shadow);
		//? } else {
		/*g.drawString(font, text, x, y, color, shadow);
		*///? }
	}

	void centered(Font font, Component text, int centerX, int y, int color) {
		text(font, text, centerX - font.width(text) / 2, y, color, false);
	}

	void centered(Font font, FormattedCharSequence text, int centerX, int y, int color) {
		text(font, text, centerX - font.width(text) / 2, y, color, false);
	}
}
