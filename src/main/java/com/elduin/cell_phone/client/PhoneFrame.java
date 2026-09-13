package com.elduin.cell_phone.client;

//? if >=26 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///? } else {
import net.minecraft.client.gui.GuiGraphics;
//? }

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The phone itself: black body, a screen, a notch, the clock and signal bars along the top.
 * CallScreen and PhoneScreen fill in the middle.
 */
abstract class PhoneFrame extends Screen {

	private static final int PHONE_W = 200;
	private static final int PHONE_MAX_H = 300;
	private static final int BEZEL = 6;

	static final int WHITE = 0xFFFFFFFF;
	static final int GREY = 0xFF9A9AA0;
	private static final int BODY = 0xFF0B0B0D;
	private static final int SCREEN = 0xFF1B2230;

	/** The lit part of the phone, inside the bezel. */
	protected int sx1;
	protected int sy1;
	protected int sx2;
	protected int sy2;

	PhoneFrame(Component title) {
		super(title);
	}

	@Override
	protected final void init() {
		int h = Math.min(this.height - 12, PHONE_MAX_H);
		int left = (this.width - PHONE_W) / 2;
		int top = (this.height - h) / 2;
		sx1 = left + BEZEL;
		sy1 = top + BEZEL;
		sx2 = left + PHONE_W - BEZEL;
		sy2 = top + h - BEZEL;
		addButtons();
	}

	/** Add this screen's buttons. Called again whenever the screen is rebuilt. */
	protected abstract void addButtons();

	/** Draw this screen's part of the phone, between the status bar and the bottom. */
	protected abstract void drawContent(Draw d);

	protected void rebuild() {
		clearWidgets();
		init();
	}

	/** The top of the area below the status bar. */
	protected int contentTop() {
		return sy1 + 16;
	}

	/** A button along the bottom of the phone. Slot 0 of 1 is full width; 0 and 1 of 2 are halves. */
	protected Button bottomButton(Component label, int slot, int of, Button.OnPress onPress) {
		int gap = 4;
		int x1 = sx1 + 8;
		int w = (sx2 - 8 - x1 - gap * (of - 1)) / of;
		return addRenderableWidget(Button.builder(label, onPress)
				.bounds(x1 + slot * (w + gap), sy2 - 30, w, 20)
				.build());
	}

	static Component red(String text) {
		return Component.literal(text).withStyle(ChatFormatting.RED);
	}

	static Component green(String text) {
		return Component.literal(text).withStyle(ChatFormatting.GREEN);
	}

	//? if >=26 {
	/*@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		drawPhone(new Draw(graphics));
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}
	*///? } else {
	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		drawPhone(new Draw(graphics));
		super.render(graphics, mouseX, mouseY, partialTick);
	}
	//? }

	private void drawPhone(Draw d) {
		int cx = (sx1 + sx2) / 2;
		d.round(sx1 - BEZEL, sy1 - BEZEL, sx2 + BEZEL, sy2 + BEZEL, BODY);
		d.fill(sx1, sy1, sx2, sy2, SCREEN);

		d.text(this.font, Component.literal(clock()), sx1 + 6, sy1 + 4, WHITE, false);
		Component bars = Component.literal("▂▄▆█");
		d.text(this.font, bars, sx2 - 6 - this.font.width(bars), sy1 + 4, WHITE, false);
		d.round(cx - 22, sy1 + 3, cx + 22, sy1 + 11, BODY);

		drawContent(d);

		d.round(cx - 24, sy2 - 5, cx + 24, sy2 - 3, 0xFF6A6A70);
	}

	private static String clock() {
		return clockAt(ClientCompat.dayTime(Minecraft.getInstance()));
	}

	/** A Minecraft time of day, the way a phone shows it. */
	static String clockAt(long dayTime) {
		long hours = (dayTime / 1000 + 6) % 24;
		long minutes = (dayTime % 1000) * 60 / 1000;
		return String.format("%d:%02d", hours, minutes);
	}

	/** Pixel villager face: skin, the famous eyebrow, green eyes and that nose. */
	private static final String[] FACE = {
			"SSSSSSSS",
			"SSSSSSSS",
			"SSSSSSSS",
			"SBBBBBBS",
			"SWGSSGWS",
			"SSSNNSSS",
			"SSSNNSSS",
			"SSSNNSSS",
			"SSSNNSSS",
			"SSSSSSSS",
	};

	/** A caller's picture: their face on a square the colour of their job. */
	protected static void drawFace(Draw d, int x, int y, int scale, Job job, int shake) {
		int pad = scale * 2;
		d.round(x - pad, y - pad, x + 8 * scale + pad, y + FACE.length * scale + pad, job.color);
		for (int row = 0; row < FACE.length; row++) {
			for (int col = 0; col < 8; col++) {
				int color = switch (FACE[row].charAt(col)) {
					case 'B' -> 0xFF3B2618;
					case 'W' -> 0xFFFFFFFF;
					case 'G' -> 0xFF2E8B3C;
					case 'N' -> 0xFF9C6D57;
					default -> 0xFFB5876E;
				};
				int px = x + shake + col * scale;
				int py = y + row * scale;
				d.fill(px, py, px + scale, py + scale, color);
			}
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
