package com.elduin.cell_phone.client.voice;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Listens to your microphone while you are on a call, and hands over each thing you say as one
 * clip: it waits for you to start talking, and cuts the clip when you stop.
 *
 * It uses OpenAL, which Minecraft already ships for its own sound, so it works on any Java the
 * game runs on. It runs on its own thread so the game never waits for it.
 */
public final class Microphone {

	public enum Status {
		OFF,
		/** Open and waiting for you to say something. */
		LISTENING,
		/** You are talking right now. */
		HEARING,
		/** The mic is open but sends nothing but silence — the computer said no to the microphone. */
		BLOCKED,
		/** There is no microphone at all. */
		MISSING
	}

	/** What whisper wants: 16,000 samples a second. OpenAL converts from whatever the mic does. */
	public static final int RATE = 16000;
	private static final int CHUNK = RATE / 20;
	private static final int PRE_ROLL_CHUNKS = 5;
	private static final int END_SILENCE_CHUNKS = 14;
	private static final int MIN_SPEECH_CHUNKS = 5;
	private static final int MAX_CHUNKS = 20 * 8;
	/** Three seconds of perfect zeros never happens with a real, allowed microphone. */
	private static final int BLOCKED_AFTER_CHUNKS = 60;
	private static final float QUIETEST_VOICE = 0.015f;

	private static volatile boolean wanted;
	private static volatile boolean muted;
	private static volatile float sensitivity = 1f;
	private static volatile Status status = Status.OFF;
	private static volatile float level;
	private static volatile Consumer<short[]> onClip = clip -> {
	};
	private static Thread thread;

	private Microphone() {
	}

	/** Start listening, if not already. Each finished thing you say goes to onClip, on the mic thread. */
	public static synchronized void listen(Consumer<short[]> onClip, float sensitivity) {
		Microphone.onClip = onClip;
		Microphone.sensitivity = Math.max(0.1f, sensitivity);
		wanted = true;
		if (thread == null) {
			thread = new Thread(Microphone::run, "Cell Phone microphone");
			thread.setDaemon(true);
			thread.start();
		}
	}

	public static void stop() {
		wanted = false;
	}

	/** While muted the mic keeps running but ignores everything — used while the villager talks. */
	public static void setMuted(boolean muted) {
		Microphone.muted = muted;
	}

	public static boolean isMuted() {
		return muted;
	}

	public static Status status() {
		return status;
	}

	/** How loud the mic is right now, 0 to 1. */
	public static float level() {
		return level;
	}

	private static void run() {
		long device = 0;
		try {
			device = ALC11.alcCaptureOpenDevice((CharSequence) null, RATE, AL10.AL_FORMAT_MONO16, RATE);
			if (device == 0) {
				status = Status.MISSING;
				while (wanted) {
					nap();
				}
			} else {
				ALC11.alcCaptureStart(device);
				status = Status.LISTENING;
				capture(device);
				ALC11.alcCaptureStop(device);
			}
		} catch (Throwable error) {
			status = Status.MISSING;
			while (wanted) {
				nap();
			}
		} finally {
			if (device != 0) {
				ALC11.alcCaptureCloseDevice(device);
			}
			finished();
		}
	}

	private static void capture(long device) {
		short[] chunk = new short[CHUNK];
		ArrayDeque<short[]> preRoll = new ArrayDeque<>();
		List<short[]> speech = new ArrayList<>();
		float noise = 0.005f;
		int quietChunks = 0;
		int zeroChunks = 0;

		while (wanted) {
			if (ALC10.alcGetInteger(device, ALC11.ALC_CAPTURE_SAMPLES) < CHUNK) {
				nap();
				continue;
			}
			ALC11.alcCaptureSamples(device, chunk, CHUNK);
			float rms = rms(chunk);
			level = rms;

			zeroChunks = rms == 0 ? zeroChunks + 1 : 0;
			boolean blocked = zeroChunks >= BLOCKED_AFTER_CHUNKS;

			if (muted || blocked) {
				speech.clear();
				preRoll.clear();
				status = blocked ? Status.BLOCKED : Status.LISTENING;
				continue;
			}

			float threshold = Math.max(QUIETEST_VOICE, noise * 3f) / sensitivity;
			if (speech.isEmpty()) {
				if (rms > threshold) {
					speech.addAll(preRoll);
					preRoll.clear();
					speech.add(chunk.clone());
					quietChunks = 0;
					status = Status.HEARING;
				} else {
					// Learn how loud the room is when nobody is talking.
					noise = noise * 0.95f + rms * 0.05f;
					preRoll.addLast(chunk.clone());
					if (preRoll.size() > PRE_ROLL_CHUNKS) {
						preRoll.removeFirst();
					}
					status = Status.LISTENING;
				}
			} else {
				speech.add(chunk.clone());
				quietChunks = rms > threshold * 0.6f ? 0 : quietChunks + 1;
				if (quietChunks >= END_SILENCE_CHUNKS || speech.size() >= MAX_CHUNKS) {
					if (speech.size() - quietChunks >= MIN_SPEECH_CHUNKS) {
						onClip.accept(join(speech));
					}
					speech.clear();
					status = Status.LISTENING;
				}
			}
		}
	}

	private static synchronized void finished() {
		thread = null;
		status = Status.OFF;
		level = 0;
		if (wanted) {
			// Someone asked to listen again while this thread was shutting down.
			listen(onClip, sensitivity);
		}
	}

	private static float rms(short[] samples) {
		double sum = 0;
		for (short s : samples) {
			double v = s / 32768.0;
			sum += v * v;
		}
		return (float) Math.sqrt(sum / samples.length);
	}

	private static short[] join(List<short[]> chunks) {
		short[] all = new short[chunks.size() * CHUNK];
		for (int i = 0; i < chunks.size(); i++) {
			System.arraycopy(chunks.get(i), 0, all, i * CHUNK, CHUNK);
		}
		return all;
	}

	private static void nap() {
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
