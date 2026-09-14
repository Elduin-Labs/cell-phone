package com.elduin.cell_phone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/** One phone call, from the first ring to hanging up. */
final class Call {

	enum State { RINGING, DIALING, TALKING, ASKING, ENDED }

	/** Who is on the other end. */
	record Caller(Component name, Job job, boolean kid, float pitch) {
	}

	/** One bubble on the phone screen. */
	record Bubble(Who who, String words, String villagerSpeak) {
	}

	enum Who { VILLAGER, YOU, PHONE }

	/** A villager sound waiting to be played, at a point on the call timer. */
	private record Hmm(int atTick, SoundEvent sound) {
	}

	/** How long the phone rings before it counts as a missed call. */
	static final int RING_TICKS = 240;
	/** How long you wait for them to pick up when you call them. */
	private static final int DIAL_TICKS = 70;
	private static final int HMM_GAP_TICKS = 11;

	/** A little rising ringtone, played on the note block xylophone. Tick in the loop, then pitch. */
	private static final float[][] RINGTONE = {{0, 1.0f}, {3, 1.26f}, {6, 1.5f}, {9, 2.0f}, {14, 1.5f}, {17, 2.0f}};
	private static final int RINGTONE_LOOP = 40;

	final Caller caller;
	final Script script;
	final boolean outgoing;
	final List<Bubble> bubbles = new ArrayList<>();

	State state;
	/** Ticks since the state last changed. */
	int stateTicks;
	/** Ticks since someone picked up. The call timer on the screen. */
	int talkTicks;
	/** What goes in the call list once this call is over. */
	CallLog.Kind outcome = CallLog.Kind.ANSWERED;

	private final Deque<Script.Line> queue = new ArrayDeque<>();
	private final List<Hmm> hmms = new ArrayList<>();
	private final Random random = new Random();
	private int nextLineIn;
	private boolean asked;
	private boolean answered;
	/** Call-timer tick when the villager's last hmm finishes. The mic ignores you until then. */
	private int speakingUntil;
	private boolean saidSignalIsBad;

	private Call(Caller caller, Script script, boolean outgoing) {
		this.caller = caller;
		this.script = script;
		this.outgoing = outgoing;
		this.state = outgoing ? State.DIALING : State.RINGING;
	}

	static Call incoming(Caller caller, Script script) {
		return new Call(caller, script, false);
	}

	static Call outgoing(Caller caller, Script script) {
		return new Call(caller, script, true);
	}

	boolean isOver() {
		return state == State.ENDED;
	}

	/** Someone picked up and nobody has hung up. This is when the microphone is on. */
	boolean isConnected() {
		return state == State.TALKING || state == State.ASKING;
	}

	/**
	 * True while the villager's voice is coming out of the speakers, so the microphone doesn't
	 * hear the villager and think it was you.
	 */
	boolean villagerSpeaking() {
		return talkTicks < speakingUntil;
	}

	/** Runs once per game tick for as long as the call exists. */
	void tick(Minecraft mc) {
		stateTicks++;
		switch (state) {
			case RINGING -> {
				int beat = stateTicks % RINGTONE_LOOP;
				for (float[] note : RINGTONE) {
					if (beat == (int) note[0]) {
						play(mc, SoundEvents.NOTE_BLOCK_XYLOPHONE.value(), note[1], 0.7f);
					}
				}
				if (stateTicks >= RING_TICKS) {
					end(CallLog.Kind.MISSED, null);
				}
			}
			case DIALING -> {
				int beat = stateTicks % 30;
				if (beat == 1 || beat == 4) {
					play(mc, SoundEvents.NOTE_BLOCK_BIT.value(), 0.7f, 0.5f);
				}
				if (stateTicks >= DIAL_TICKS) {
					queue.add(Script.yes("Hmm? Oh! You called me!"));
					pickUp();
				}
			}
			case TALKING -> {
				talkTicks++;
				playDueHmms(mc);
				if (--nextLineIn > 0) {
					return;
				}
				if (!queue.isEmpty()) {
					say(queue.poll());
				} else if (!asked && script.question() != null) {
					asked = true;
					say(Script.hmm(script.question()));
				} else if (asked && !answered) {
					setState(State.ASKING);
				} else {
					end(CallLog.Kind.ANSWERED, caller.name().getString() + " hung up.");
				}
			}
			case ASKING -> {
				talkTicks++;
				playDueHmms(mc);
			}
			case ENDED -> {
			}
		}
	}

	/** The green button. */
	void answer() {
		if (state == State.RINGING) {
			pickUp();
		}
	}

	/** Your reply to the villager's question, from the Yes and No buttons. */
	void reply(boolean yes) {
		reply(yes, yes ? "Yes!" : "No.");
	}

	private void reply(boolean yes, String yourWords) {
		if (state != State.ASKING) {
			return;
		}
		answered = true;
		bubbles.add(new Bubble(Who.YOU, yourWords, null));
		queue.addAll(yes ? script.ifYes() : script.ifNo());
		nextLineIn = 20;
		setState(State.TALKING);
	}

	/**
	 * You said something out loud. words is what whisper made of it, or null if this computer
	 * can't turn voices into words — then the villager knows you talked, but not what you said.
	 */
	void hear(String words) {
		if (!isConnected()) {
			return;
		}
		if (words == null) {
			bubbles.add(new Bubble(Who.YOU, "(you talked)", null));
			if (!saidSignalIsBad) {
				saidSignalIsBad = true;
				Script.Line line = Script.no("Hrmm? The signal is bad. I only hear hmms.");
				if (state == State.ASKING) {
					say(line);
				} else {
					interrupt(new Replies.Reaction(List.of(line), false));
				}
			}
			return;
		}
		String yourWords = capitalise(words);
		if (state == State.ASKING) {
			switch (Replies.yesOrNo(words)) {
				case YES -> reply(true, yourWords);
				case NO -> reply(false, yourWords);
				case UNSURE -> {
					bubbles.add(new Bubble(Who.YOU, yourWords, null));
					Replies.Reaction reaction = Replies.reactTo(words, caller, random);
					if (reaction.hangUp()) {
						// Saying goodbye instead of answering is an answer too.
						setState(State.TALKING);
						interrupt(reaction);
					} else {
						say(Script.hmm("Hmm? Is that a yes or a no?"));
					}
				}
			}
			return;
		}
		bubbles.add(new Bubble(Who.YOU, yourWords, null));
		interrupt(Replies.reactTo(words, caller, random));
	}

	/** The villager answers you as soon as they finish the line they're on, then carries on. */
	private void interrupt(Replies.Reaction reaction) {
		if (reaction.hangUp()) {
			queue.clear();
			asked = true;
			answered = true;
		}
		List<Script.Line> lines = reaction.lines();
		for (int i = lines.size() - 1; i >= 0; i--) {
			queue.addFirst(lines.get(i));
		}
		if (state == State.TALKING) {
			nextLineIn = Math.min(nextLineIn, 20);
		}
	}

	private static String capitalise(String words) {
		String trimmed = words.length() > 120 ? words.substring(0, 117) + "..." : words;
		return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
	}

	/** The red button, or closing the phone mid-call. */
	void hangUp() {
		switch (state) {
			case RINGING -> end(CallLog.Kind.DECLINED, "You declined the call.");
			case DIALING -> end(CallLog.Kind.CALLED, "You hung up.");
			case TALKING, ASKING -> end(CallLog.Kind.ANSWERED, "You hung up.");
			case ENDED -> {
			}
		}
	}

	private void pickUp() {
		queue.addAll(script.opening());
		nextLineIn = 15;
		setState(State.TALKING);
	}

	private void say(Script.Line line) {
		bubbles.add(new Bubble(Who.VILLAGER, line.words(), line.villagerSpeak()));
		int count = line.hmmCount();
		for (int i = 0; i < count; i++) {
			// Every hmm is ordinary except the last, which carries the mood of the line.
			SoundEvent sound = i == count - 1 ? line.mood().sound : SoundEvents.VILLAGER_AMBIENT;
			hmms.add(new Hmm(talkTicks + i * HMM_GAP_TICKS, sound));
		}
		// A villager sound lasts about a second; leave that long after the last one starts.
		speakingUntil = Math.max(speakingUntil, talkTicks + (count - 1) * HMM_GAP_TICKS + 22);
		nextLineIn = line.readingTicks();
	}

	private void playDueHmms(Minecraft mc) {
		hmms.removeIf(hmm -> {
			if (hmm.atTick() > talkTicks) {
				return false;
			}
			// A little wobble, so four hmms in a row don't sound like a recording.
			float wobble = 0.94f + random.nextFloat() * 0.12f;
			play(mc, hmm.sound(), caller.pitch() * wobble, 1.0f);
			return true;
		});
	}

	private void end(CallLog.Kind kind, String message) {
		if (message != null) {
			bubbles.add(new Bubble(Who.PHONE, message, null));
		}
		if (outgoing) {
			kind = CallLog.Kind.CALLED;
		}
		outcome = kind;
		hmms.clear();
		setState(State.ENDED);
	}

	private void setState(State next) {
		state = next;
		stateTicks = 0;
	}

	private static void play(Minecraft mc, SoundEvent sound, float pitch, float volume) {
		mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
	}
}
