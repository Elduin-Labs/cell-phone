package com.elduin.cell_phone.client;

import com.elduin.cell_phone.client.voice.Microphone;
import com.elduin.cell_phone.client.voice.Transcriber;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * A call on the phone screen. Ringing shows a big face with Answer and Decline. Once you pick
 * up, the villager's hmms come up as bubbles with the real words under them.
 */
class CallScreen extends PhoneFrame {

	/** How long "Call ended" stays up before the phone puts itself away. */
	private static final int CLOSE_AFTER_TICKS = 80;

	private static final int VILLAGER_BUBBLE = 0xFFEDE6D6;
	private static final int VILLAGER_SPEAK = 0xFF8A7760;
	private static final int VILLAGER_WORDS = 0xFF1F1F1F;
	private static final int YOUR_BUBBLE = 0xFF34C759;

	private final Call call;
	private Call.State shownState;
	private int endedTicks;

	CallScreen(Call call) {
		super(Component.literal("Call"));
		this.call = call;
	}

	@Override
	protected void addButtons() {
		shownState = call.state;
		switch (call.state) {
			case RINGING -> {
				bottomButton(red("Decline"), 0, 2, b -> call.hangUp());
				bottomButton(green("Answer"), 1, 2, b -> call.answer());
			}
			case DIALING, TALKING -> bottomButton(red("Hang up"), 0, 1, b -> call.hangUp());
			case ASKING -> {
				bottomButton(Component.literal("No"), 0, 2, b -> call.reply(false));
				bottomButton(Component.literal("Yes"), 1, 2, b -> call.reply(true));
			}
			case ENDED -> bottomButton(Component.literal("Close"), 0, 1, b -> onClose());
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (call.state != shownState) {
			rebuild();
		}
		if (call.isOver() && ++endedTicks >= CLOSE_AFTER_TICKS) {
			onClose();
		}
	}

	@Override
	public void onClose() {
		// Walking away from a ringing phone leaves it ringing. Walking away mid-call hangs up.
		if (call.state != Call.State.RINGING) {
			call.hangUp();
		}
		ClientCompat.setScreen(Minecraft.getInstance(), null);
	}

	@Override
	protected void drawContent(Draw d) {
		if (call.state == Call.State.RINGING || call.state == Call.State.DIALING) {
			drawRinging(d);
		} else {
			drawTalking(d);
		}
	}

	private void drawRinging(Draw d) {
		int cx = (sx1 + sx2) / 2;
		int scale = 5;
		int faceTop = contentTop() + 30;

		// The face buzzes at the start of every ring, like a phone vibrating on a table.
		int shake = 0;
		if (call.state == Call.State.RINGING && call.stateTicks % 40 < 12) {
			shake = (call.stateTicks / 2) % 2 == 0 ? -1 : 1;
		}
		drawFace(d, cx - 4 * scale, faceTop, scale, call.caller.job(), shake);

		int y = faceTop + 10 * scale + 20;
		d.centered(this.font, call.caller.name(), cx, y, WHITE);
		String status = call.state == Call.State.RINGING ? "is calling you..." : "Calling...";
		d.centered(this.font, Component.literal(status), cx, y + 13, GREY);
	}

	private void drawTalking(Draw d) {
		int top = contentTop();
		drawFace(d, sx1 + 12, top + 6, 2, call.caller.job(), 0);
		d.text(this.font, call.caller.name(), sx1 + 38, top + 6, WHITE, false);
		String status = call.isOver() ? "Call ended" : timer(call.talkTicks);
		d.text(this.font, Component.literal(status), sx1 + 38, top + 17, GREY, false);
		d.fill(sx1 + 6, top + 34, sx2 - 6, top + 35, 0xFF2E3747);

		if (call.isConnected() && PhoneClient.config.microphone) {
			drawMicrophone(d, sy2 - 46);
			drawBubbles(d, top + 40, sy2 - 52);
		} else {
			drawBubbles(d, top + 40, sy2 - 36);
		}
	}

	/** A little mic, a loudness bar, and what the phone is doing with your voice. */
	private void drawMicrophone(Draw d, int y) {
		int x = sx1 + 10;
		Microphone.Status status = Microphone.status();
		boolean live = status == Microphone.Status.LISTENING || status == Microphone.Status.HEARING;
		boolean hearing = status == Microphone.Status.HEARING && !Microphone.isMuted();
		int micColor = hearing ? 0xFF34C759 : live ? WHITE : GREY;

		// Pixel microphone: head, stand, base.
		d.round(x + 2, y, x + 7, y + 6, micColor);
		d.fill(x + 4, y + 6, x + 5, y + 8, micColor);
		d.fill(x + 2, y + 8, x + 7, y + 9, micColor);

		String text;
		if (Transcriber.busy()) {
			text = "Thinking...";
		} else if (status == Microphone.Status.MISSING) {
			text = "No microphone found";
		} else if (status == Microphone.Status.BLOCKED) {
			text = "Mic blocked in Settings";
		} else if (call.villagerSpeaking()) {
			text = "They're talking...";
		} else if (hearing) {
			text = "Hearing you...";
		} else if (call.state == Call.State.ASKING) {
			text = "Say yes or no!";
		} else {
			text = "Talk to them!";
		}
		d.text(this.font, Component.literal(text), x + 12, y + 1, micColor, false);

		if (live) {
			int barX2 = sx2 - 10;
			int barX1 = barX2 - 30;
			d.fill(barX1, y + 3, barX2, y + 6, 0xFF2E3747);
			float loud = Microphone.isMuted() ? 0 : Math.min(1f, Microphone.level() * 12f);
			d.fill(barX1, y + 3, barX1 + Math.round(30 * loud), y + 6, 0xFF34C759);
		}
	}

	/** Newest at the bottom, working upwards until there is no room left. */
	private void drawBubbles(Draw d, int areaTop, int areaBottom) {
		int maxTextW = (sx2 - sx1) - 44;
		int lineH = this.font.lineHeight + 1;
		int y = areaBottom;

		List<Call.Bubble> bubbles = call.bubbles;
		for (int i = bubbles.size() - 1; i >= 0; i--) {
			Call.Bubble bubble = bubbles.get(i);
			List<FormattedCharSequence> speak = bubble.villagerSpeak() == null ? List.of()
					: this.font.split(Component.literal(bubble.villagerSpeak()).withStyle(ChatFormatting.ITALIC), maxTextW);
			List<FormattedCharSequence> words = this.font.split(Component.literal(bubble.words()), maxTextW);

			if (bubble.who() == Call.Who.PHONE) {
				int h = words.size() * lineH;
				if (y - h < areaTop) {
					break;
				}
				for (int l = 0; l < words.size(); l++) {
					d.centered(this.font, words.get(l), (sx1 + sx2) / 2, y - h + l * lineH, GREY);
				}
				y -= h + 5;
				continue;
			}

			int textW = 0;
			for (FormattedCharSequence line : speak) {
				textW = Math.max(textW, this.font.width(line));
			}
			for (FormattedCharSequence line : words) {
				textW = Math.max(textW, this.font.width(line));
			}
			int h = (speak.size() + words.size()) * lineH + 7;
			int bubbleTop = y - h;
			if (bubbleTop < areaTop) {
				break;
			}

			boolean villager = bubble.who() == Call.Who.VILLAGER;
			int x1 = villager ? sx1 + 8 : sx2 - 8 - textW - 10;
			d.round(x1, bubbleTop, x1 + textW + 10, y, villager ? VILLAGER_BUBBLE : YOUR_BUBBLE);

			int ty = bubbleTop + 4;
			for (FormattedCharSequence line : speak) {
				d.text(this.font, line, x1 + 5, ty, VILLAGER_SPEAK, false);
				ty += lineH;
			}
			for (FormattedCharSequence line : words) {
				d.text(this.font, line, x1 + 5, ty, villager ? VILLAGER_WORDS : WHITE, false);
				ty += lineH;
			}
			y = bubbleTop - 5;
		}
	}

	private static String timer(int ticks) {
		int seconds = ticks / 20;
		return String.format("%d:%02d", seconds / 60, seconds % 60);
	}
}
