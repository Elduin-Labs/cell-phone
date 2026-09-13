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

	/** Your reply to the villager's question. */
	void reply(boolean yes) {
		if (state != State.ASKING) {
			return;
		}
		answered = true;
		bubbles.add(new Bubble(Who.YOU, yes ? "Yes!" : "No.", null));
		queue.addAll(yes ? script.ifYes() : script.ifNo());
		nextLineIn = 20;
		setState(State.TALKING);
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
