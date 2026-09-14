package com.elduin.cell_phone.client;

import static com.elduin.cell_phone.client.Script.*;

import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * What villagers say back when you talk to them out loud. They only know a few words of human,
 * so they listen for those and hmm politely at everything else.
 */
final class Replies {

	enum Answer { YES, NO, UNSURE }

	/** Lines to say, and whether to hang up after them. */
	record Reaction(List<Line> lines, boolean hangUp) {
	}

	private static final String[] NO_PHRASES = {
			"no way", "not really", "i will not", "i won't", "i can't", "i cannot", "i don't", "i do not", "not now"};
	private static final String[] NO_WORDS = {"no", "nope", "nah", "never", "nuh"};
	private static final String[] YES_PHRASES = {
			"of course", "i will", "i can", "i do", "let's go", "why not", "uh huh", "for sure"};
	private static final String[] YES_WORDS = {
			"yes", "yeah", "yep", "yup", "yea", "ya", "sure", "ok", "okay", "alright", "definitely",
			"absolutely", "certainly", "totally", "fine"};

	private Replies() {
	}

	/** Did you just say yes or no? Something like "no way" counts as no even though it has no "no" word. */
	static Answer yesOrNo(String words) {
		String text = normalise(words);
		if (containsAny(text, NO_PHRASES)) {
			return Answer.NO;
		}
		for (String word : text.trim().split(" ")) {
			if (isOneOf(word, NO_WORDS)) {
				return Answer.NO;
			}
			if (isOneOf(word, YES_WORDS)) {
				return Answer.YES;
			}
		}
		return containsAny(text, YES_PHRASES) ? Answer.YES : Answer.UNSURE;
	}

	/** You said something while the villager was talking. They answer, then carry on. */
	static Reaction reactTo(String words, Call.Caller caller, Random random) {
		String text = normalise(words);
		String name = caller.name().getString();

		if (containsAny(text, "bye", "goodbye", "see you", "see ya", "hang up", "gotta go")) {
			return new Reaction(List.of(yes("Okay. Goodbye, human!")), true);
		}
		if (containsAny(text, "hello", "hi", "hey", "hiya", "howdy")) {
			return say(hmm("Hello! Hmm. It is " + name + "."));
		}
		if (containsAny(text, "how are you", "how's it going", "what's up")) {
			return say(yes("I am good. Thank you for asking."));
		}
		if (containsAny(text, "who are you", "your name", "who is this", "who's this")) {
			return say(hmm("It is me! " + name + "! From the village!"));
		}
		if (containsAny(text, "emerald", "trade", "buy", "sell", "money")) {
			return say(trade("Emeralds? Now you are speaking my language."));
		}
		if (containsAny(text, "zombie", "creeper", "pillager", "skeleton", "raid", "witch")) {
			return say(ouch("Where?! Do not scare me like that!"));
		}
		if (containsAny(text, "love you", "like you", "friend", "cool", "awesome")) {
			return say(happy("Aww. You are my best friend."));
		}
		if (containsAny(text, "hmm", "hrm", "hrmm")) {
			return say(happy("You speak villager! Hmm hmm!"));
		}
		if (containsAny(text, "what", "huh", "pardon", "sorry")) {
			return say(hmm("I said... hmm. I forgot what I said."));
		}
		List<Line> shrugs = List.of(
				hmm("Hmm. Interesting."),
				hmm("Hmm hmm. I see."),
				no("Hrmm? The signal is bad. Say that again?"),
				hmm("That is very human of you."),
				yes("Hmm! I will tell the other villagers."));
		return say(shrugs.get(random.nextInt(shrugs.size())));
	}

	private static Reaction say(Line line) {
		return new Reaction(List.of(line), false);
	}

	/** Lower case, punctuation gone except apostrophes, and a space at each end for whole-word matching. */
	private static String normalise(String words) {
		String text = words.toLowerCase(Locale.ROOT)
				.replace('’', '\'')
				.replaceAll("[^a-z0-9' ]", " ")
				.replaceAll("\\s+", " ")
				.trim();
		return " " + text + " ";
	}

	private static boolean containsAny(String normalised, String... phrases) {
		for (String phrase : phrases) {
			if (normalised.contains(" " + phrase + " ")) {
				return true;
			}
		}
		return false;
	}

	private static boolean isOneOf(String word, String[] words) {
		for (String w : words) {
			if (w.equals(word)) {
				return true;
			}
		}
		return false;
	}
}
