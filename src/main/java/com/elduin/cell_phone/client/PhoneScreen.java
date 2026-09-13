package com.elduin.cell_phone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** The phone when nobody is calling: your recent calls, with a button to call each one back. */
class PhoneScreen extends PhoneFrame {

	private static final int ROW_H = 28;
	private static final int MISSED = 0xFFFF6B6B;

	private List<CallLog.Entry> entries = List.of();

	PhoneScreen() {
		super(Component.literal("Phone"));
	}

	@Override
	protected void addButtons() {
		entries = CallLog.entries().subList(0, Math.min(CallLog.entries().size(), rowsThatFit()));
		for (int i = 0; i < entries.size(); i++) {
			Call.Caller caller = entries.get(i).caller();
			addRenderableWidget(Button.builder(green("Call"),
							b -> PhoneClient.callBack(Minecraft.getInstance(), caller))
					.bounds(sx2 - 48, rowTop(i) + 3, 40, 18)
					.build());
		}
		bottomButton(Component.literal("Close"), 0, 1, b -> onClose());
	}

	@Override
	protected void drawContent(Draw d) {
		d.text(this.font, Component.literal("Recent Calls"), sx1 + 10, contentTop() + 4, WHITE, false);

		if (entries.isEmpty()) {
			List<FormattedCharSequence> lines = this.font.split(FormattedText.of(
					"No calls yet. Keep your phone in your inventory and the villagers will call you."),
					sx2 - sx1 - 30);
			int y = (sy1 + sy2) / 2 - lines.size() * 5;
			for (FormattedCharSequence line : lines) {
				d.centered(this.font, line, (sx1 + sx2) / 2, y, GREY);
				y += 10;
			}
			return;
		}

		for (int i = 0; i < entries.size(); i++) {
			CallLog.Entry entry = entries.get(i);
			int y = rowTop(i);
			d.fill(sx1 + 6, y, sx2 - 6, y + 1, 0xFF2E3747);
			boolean missed = entry.kind() == CallLog.Kind.MISSED;
			d.text(this.font, entry.caller().name(), sx1 + 10, y + 4, missed ? MISSED : WHITE, false);
			d.text(this.font, Component.literal(entry.kind().label + " · " + clockAt(entry.dayTime())),
					sx1 + 10, y + 15, GREY, false);
		}
	}

	private int rowTop(int row) {
		return contentTop() + 18 + row * ROW_H;
	}

	private int rowsThatFit() {
		return Math.max(0, (sy2 - 36 - rowTop(0)) / ROW_H);
	}
}
