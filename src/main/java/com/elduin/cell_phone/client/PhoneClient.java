package com.elduin.cell_phone.client;

import com.elduin.cell_phone.CellPhoneItem;
import com.elduin.cell_phone.PhoneItems;
import com.elduin.cell_phone.client.voice.Microphone;
import com.elduin.cell_phone.client.voice.Transcriber;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.List;
import java.util.Random;

/** Decides when villagers call, and keeps the call going while it happens. */
public final class PhoneClient {

	/** The first call comes quickly, so you find out what the phone does. */
	private static final int FIRST_CALL_TICKS = 20 * 20;
	/** After that, somewhere between one and a half and four minutes between calls. */
	private static final int MIN_GAP_TICKS = 20 * 90;
	private static final int EXTRA_GAP_TICKS = 20 * 150;
	/** A villager this close can be the one calling. Further away, a random villager calls. */
	private static final double NEARBY = 96;

	private static final Random RANDOM = new Random();

	/** The call that is ringing, dialling or talking. Null when the phone is quiet. */
	private static volatile Call active;
	private static int ticksUntilCall = -1;
	static PhoneConfig config;

	private PhoneClient() {
	}

	public static void register() {
		config = PhoneConfig.load();
		CellPhoneItem.openOnClient = player -> openPhone(Minecraft.getInstance());
		ClientTickEvents.END_CLIENT_TICK.register(PhoneClient::tick);
	}

	private static void tick(Minecraft mc) {
		tickCalls(mc);
		tickMicrophone();
	}

	private static void tickCalls(Minecraft mc) {
		if (mc.player == null || mc.level == null) {
			// Left the world. Hang everything up and start fresh next time.
			active = null;
			ticksUntilCall = -1;
			CallLog.clear();
			return;
		}
		if (mc.isPaused()) {
			return;
		}

		if (active != null) {
			active.tick(mc);
			Screen screen = ClientCompat.screen(mc);
			if (active.state == Call.State.RINGING && active.stateTicks % 20 == 1
					&& !(screen instanceof CallScreen)) {
				ClientCompat.actionBar(mc, Component.literal("☎ ")
						.append(active.caller.name())
						.append(" is calling! Right-click your phone to answer.")
						.withStyle(ChatFormatting.GREEN));
			}
			if (active.isOver()) {
				finish(mc, active);
			}
			return;
		}

		if (!PhoneItems.carrying(mc.player)) {
			return;
		}
		if (ticksUntilCall < 0) {
			ticksUntilCall = FIRST_CALL_TICKS;
		}
		if (--ticksUntilCall > 0) {
			return;
		}

		Call.Caller caller = pickCaller(mc);
		active = Call.incoming(caller, Scripts.pick(caller.job(), caller.kid(), RANDOM));
		if (ClientCompat.screen(mc) instanceof PhoneScreen) {
			ClientCompat.setScreen(mc, new CallScreen(active));
		}
	}

	/** The microphone is on only while you're connected to someone, and deaf while they talk. */
	private static void tickMicrophone() {
		Call call = active;
		if (call == null || !call.isConnected() || !config.microphone) {
			Microphone.stop();
			return;
		}
		Microphone.listen(PhoneClient::heardSomething, config.sensitivity);
		Microphone.setMuted(call.villagerSpeaking() || Transcriber.busy());
	}

	/** Runs on the microphone thread with one thing you said. */
	private static void heardSomething(short[] clip) {
		Call call = active;
		if (call == null) {
			return;
		}
		Transcriber.transcribe(clip, config.whisper, config.model, PhoneConfig.modelFolder(), words -> {
			if (words != null && words.isEmpty()) {
				return;
			}
			Minecraft.getInstance().execute(() -> {
				if (active == call) {
					call.hear(words);
				}
			});
		});
	}

	/** Right-clicking the phone. */
	static void openPhone(Minecraft mc) {
		ClientCompat.setScreen(mc, active != null ? new CallScreen(active) : new PhoneScreen());
	}

	/** The Call button next to someone in your recent calls. */
	static void callBack(Minecraft mc, Call.Caller caller) {
		if (active != null) {
			return;
		}
		active = Call.outgoing(caller, Scripts.pick(caller.job(), caller.kid(), RANDOM));
		ClientCompat.setScreen(mc, new CallScreen(active));
	}

	private static void finish(Minecraft mc, Call call) {
		CallLog.add(call.caller, call.outcome, ClientCompat.dayTime(mc));
		if (call.outcome == CallLog.Kind.MISSED) {
			ClientCompat.actionBar(mc, Component.literal("☎ Missed call from ")
					.append(call.caller.name())
					.withStyle(ChatFormatting.RED));
		}
		active = null;
		ticksUntilCall = MIN_GAP_TICKS + RANDOM.nextInt(EXTRA_GAP_TICKS);
	}

	private static Call.Caller pickCaller(Minecraft mc) {
		List<Villager> nearby = mc.level.getEntitiesOfClass(Villager.class,
				mc.player.getBoundingBox().inflate(NEARBY), Villager::isAlive);
		if (!nearby.isEmpty()) {
			Villager villager = nearby.get(RANDOM.nextInt(nearby.size()));
			Job job = Job.of(villager);
			boolean kid = villager.isBaby();
			Component name = villager.hasCustomName() ? villager.getCustomName()
					: kid ? Component.literal("Kid Villager")
					: job.title();
			return new Call.Caller(name, job, kid, voicePitch(kid));
		}
		Job job = Job.values()[RANDOM.nextInt(Job.values().length)];
		return new Call.Caller(job.title(), job, false, voicePitch(false));
	}

	/** Same spread Minecraft uses for villager voices. Kids are much squeakier. */
	private static float voicePitch(boolean kid) {
		return kid ? 1.5f : 0.85f + RANDOM.nextFloat() * 0.3f;
	}
}
