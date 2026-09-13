package com.elduin.cell_phone.client;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * One phone call: some things the villager says, maybe a question for you, and what they say
 * back depending on your answer.
 */
record Script(List<Line> opening, String question, List<Line> ifYes, List<Line> ifNo) {

	/** How the villager sounds while saying a line. Every one is Mojang's own villager sound. */
	enum Mood {
		HMM(SoundEvents.VILLAGER_AMBIENT),
		YES(SoundEvents.VILLAGER_YES),
		NO(SoundEvents.VILLAGER_NO),
		TRADE(SoundEvents.VILLAGER_TRADE),
		HAPPY(SoundEvents.VILLAGER_CELEBRATE),
		OUCH(SoundEvents.VILLAGER_HURT);

		final SoundEvent sound;

		Mood(SoundEvent sound) {
			this.sound = sound;
		}
	}

	/** Something the villager says: the real words, and how it sounds. */
	record Line(String words, Mood mood) implements Part {

		private static final String[] HMMS = {"Hmm", "Hrmm", "Hmmh", "Hurr", "Hm", "Hrrm", "Hmmm", "Huh"};

		/**
		 * What you actually hear, written down: about one hmm for every two real words. The
		 * same words always come out as the same hmms, so a line never changes between calls.
		 */
		String villagerSpeak() {
			String[] real = words.trim().split("\\s+");
			int count = Math.max(1, (real.length + 1) / 2);
			Random random = new Random(words.hashCode());
			StringBuilder out = new StringBuilder();
			for (int i = 0; i < count; i++) {
				String hmm = HMMS[random.nextInt(HMMS.length)];
				out.append(i == 0 ? hmm : " " + hmm.toLowerCase());
			}
			char last = words.charAt(words.length() - 1);
			out.append(last == '?' || last == '!' ? last : '.');
			return out.toString();
		}

		/** How many hmms you hear. Long lines get a few, short lines get one. */
		int hmmCount() {
			return Math.min(4, Math.max(1, Math.round(words.length() / 22f)));
		}

		/** How long to leave the line up before the next one, in ticks. */
		int readingTicks() {
			return Math.min(140, Math.max(50, 30 + words.length() * 2));
		}
	}

	// ---- a tiny way of writing calls down, used by Scripts ----

	sealed interface Part permits Line, Ask, Answer {
	}

	record Ask(String question) implements Part {
	}

	record Answer(boolean yes, List<Line> lines) implements Part {
	}

	static Line hmm(String words) {
		return new Line(words, Mood.HMM);
	}

	static Line yes(String words) {
		return new Line(words, Mood.YES);
	}

	static Line no(String words) {
		return new Line(words, Mood.NO);
	}

	static Line trade(String words) {
		return new Line(words, Mood.TRADE);
	}

	static Line happy(String words) {
		return new Line(words, Mood.HAPPY);
	}

	static Line ouch(String words) {
		return new Line(words, Mood.OUCH);
	}

	static Ask ask(String question) {
		return new Ask(question);
	}

	static Answer ifYes(Line... lines) {
		return new Answer(true, List.of(lines));
	}

	static Answer ifNo(Line... lines) {
		return new Answer(false, List.of(lines));
	}

	static Script of(Part... parts) {
		List<Line> opening = new ArrayList<>();
		String question = null;
		List<Line> yes = List.of();
		List<Line> no = List.of();
		for (Part part : parts) {
			switch (part) {
				case Line line -> opening.add(line);
				case Ask a -> question = a.question();
				case Answer answer -> {
					if (answer.yes()) {
						yes = answer.lines();
					} else {
						no = answer.lines();
					}
				}
			}
		}
		return new Script(List.copyOf(opening), question, yes, no);
	}
}
