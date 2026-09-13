package com.elduin.cell_phone.client;

import static com.elduin.cell_phone.client.Script.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Every phone call a villager can make. To add a call, copy one of these and change the words.
 *
 * hmm(...)   an ordinary villager hmm
 * yes(...)   the happy "hm-hmm!" villagers make when you trade
 * no(...)    the grumpy "hrmm" villagers make when you can't trade
 * trade(...) the sound a villager makes while you look at their trades
 * happy(...) the cheer villagers do when a raid is over
 * ouch(...)  the sound a villager makes when it gets hurt
 */
final class Scripts {

	private static final Map<Job, List<Script>> BY_JOB = new EnumMap<>(Job.class);
	private static final List<Script> KIDS = new ArrayList<>();

	private Scripts() {
	}

	static Script pick(Job job, boolean kid, Random random) {
		List<Script> scripts = kid ? KIDS : BY_JOB.get(job);
		return scripts.get(random.nextInt(scripts.size()));
	}

	private static void add(Job job, Part... parts) {
		BY_JOB.computeIfAbsent(job, j -> new ArrayList<>()).add(Script.of(parts));
	}

	private static void kid(Part... parts) {
		KIDS.add(Script.of(parts));
	}

	static {
		add(Job.FARMER,
				hmm("Hello? Is this the human?"),
				no("Something is eating my carrots."),
				hmm("I think it is a rabbit. A very big rabbit."),
				ask("Can you come scare it away?"),
				ifYes(yes("Thank you! I will hide in my house until you get here.")),
				ifNo(no("Then I will eat potatoes forever. Goodbye.")));
		add(Job.FARMER,
				hmm("Hello. It is me, the farmer."),
				happy("I grew a pumpkin as big as a house!"),
				ask("Do you want to come see it?"),
				ifYes(yes("Come quick, before a pillager steals it!")),
				ifNo(no("Fine. I will show it to the cows instead.")));

		add(Job.LIBRARIAN,
				hmm("Shhh. I am calling from the library."),
				no("Somebody put a creeper book on my shelf."),
				ouch("It hissed at me!"),
				ask("Was it you?"),
				ifYes(no("Hmph! No more books for you.")),
				ifNo(hmm("Then who did it? The mystery grows.")));
		add(Job.LIBRARIAN,
				trade("Hello. I have a Mending book for you."),
				trade("It only costs forty emeralds."),
				ask("Deal?"),
				ifYes(yes("Wonderful! Bring the emeralds.")),
				ifNo(no("Thirty-nine, then. That is my final offer.")));

		add(Job.ARMORER,
				hmm("Hello. This is the armorer."),
				hmm("I saw your armor today. It is very scratched."),
				hmm("Did a zombie chew on it?"),
				ask("Do you want me to fix it?"),
				ifYes(yes("Come to my blast furnace. I will make it shiny.")),
				ifNo(no("Okay. But do not cry when a skeleton shoots you.")));

		add(Job.BUTCHER,
				hmm("Hello, it is the butcher."),
				hmm("I have too many porkchops."),
				no("Like, WAY too many."),
				ask("Do you want some?"),
				ifYes(happy("Great! I will throw them at your house.")),
				ifNo(hmm("Hmm. The pigs will be happy about that.")));

		add(Job.CARTOGRAPHER,
				hmm("Hello! Cartographer here."),
				trade("I found a map to a secret place."),
				hmm("It says X marks the spot."),
				ask("Do you want to go treasure hunting?"),
				ifYes(happy("Bring a shovel! And snacks!")),
				ifNo(hmm("Then I will go by myself."), no("Hmm. Which way is north?")));

		add(Job.CLERIC,
				hmm("Greetings. It is the cleric."),
				hmm("I sense... a zombie near your house."),
				trade("Also, I sell redstone."),
				ask("Do you need a potion?"),
				ifYes(yes("Bring me rotten flesh. I love it, for some reason.")),
				ifNo(hmm("Very well. Be safe out there.")));

		add(Job.FISHERMAN,
				hmm("Hello? I am calling you from a boat."),
				happy("I just caught a fish this big!"),
				no("No, BIGGER."),
				ask("Do you believe me?"),
				ifYes(happy("Finally, somebody believes me!")),
				ifNo(no("It was real! It got away!")));

		add(Job.FLETCHER,
				hmm("Hello, it is the fletcher."),
				no("I need sticks. So many sticks."),
				ask("Can you bring me some sticks?"),
				ifYes(yes("You are my best friend.")),
				ifNo(hmm("Then I will make arrows out of..."), no("Carrots? Hmm.")));

		add(Job.LEATHERWORKER,
				hmm("Hi. It is the leatherworker."),
				trade("I made you a leather hat."),
				hmm("It is a little bit ugly."),
				ask("Do you still want it?"),
				ifYes(yes("Yay! It looks great on you. Kind of.")),
				ifNo(no("Okay. I will give it to a chicken.")));

		add(Job.MASON,
				hmm("Hello, it is the mason."),
				hmm("I built a wall."),
				happy("It is a very good wall."),
				ask("Do you want to come and look at my wall?"),
				ifYes(happy("It is grey. And it is square. You will love it.")),
				ifNo(no("Nobody ever wants to see my wall.")));

		add(Job.SHEPHERD,
				hmm("Hello! Shepherd here."),
				no("One of my sheep turned pink."),
				hmm("I did not do it. Probably."),
				ask("Is pink a good colour for a sheep?"),
				ifYes(happy("I knew it! Now I will make them ALL pink.")),
				ifNo(no("Oh no. I will go and get more dye.")));

		add(Job.TOOLSMITH,
				hmm("Hello. The toolsmith is calling."),
				ask("Did you break your pickaxe again?"),
				ifYes(no("Hmm. Come and get a new one. Be gentle this time.")),
				ifNo(yes("Good. Keep it that way.")));

		add(Job.WEAPONSMITH,
				ouch("Hello! It is the weaponsmith!"),
				ouch("Pillagers are coming to the village!"),
				hmm("We need a hero."),
				ask("Will you help us?"),
				ifYes(happy("Hooray! Bring your best sword!")),
				ifNo(no("Then we will all hide in the bell tower.")));

		add(Job.UNEMPLOYED,
				hmm("Hmm. Hello."),
				no("I do not have a job."),
				hmm("I just walk around all day."),
				ask("Can you give me a job?"),
				ifYes(happy("Really? I will go and find a workstation right now!")),
				ifNo(hmm("Okay. More walking, then.")));

		add(Job.NITWIT,
				hmm("Hello? Is this the pizza place?"),
				ask("Is it?"),
				ifYes(trade("I would like one pizza with extra wheat seeds.")),
				ifNo(no("Wrong number. Bye.")));
		add(Job.NITWIT,
				hmm("Hmm?"),
				hmm("Hmm hmm."),
				ouch("Oops. I sat on my phone."));

		kid(
				happy("Hehe. Hi!"),
				ask("Is your refrigerator running?"),
				ifYes(happy("Then you better go catch it! Hehehe!")),
				ifNo(no("Hmm. That is not how the joke goes.")));
		kid(
				hmm("Hi! I am playing tag."),
				happy("You are IT!"));
	}
}
