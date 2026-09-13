package com.elduin.cell_phone.client;

import java.util.ArrayList;
import java.util.List;

/** Recent calls, newest first. Forgotten when you leave the world, like a phone with no memory. */
final class CallLog {

	enum Kind {
		MISSED("Missed call"),
		ANSWERED("Answered"),
		DECLINED("Declined"),
		CALLED("You called");

		final String label;

		Kind(String label) {
			this.label = label;
		}
	}

	record Entry(Call.Caller caller, Kind kind, long dayTime) {
	}

	private static final int KEEP = 8;
	private static final List<Entry> ENTRIES = new ArrayList<>();

	private CallLog() {
	}

	static void add(Call.Caller caller, Kind kind, long dayTime) {
		ENTRIES.addFirst(new Entry(caller, kind, dayTime));
		while (ENTRIES.size() > KEEP) {
			ENTRIES.removeLast();
		}
	}

	static List<Entry> entries() {
		return List.copyOf(ENTRIES);
	}

	static void clear() {
		ENTRIES.clear();
	}
}
