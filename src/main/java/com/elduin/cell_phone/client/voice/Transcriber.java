package com.elduin.cell_phone.client.voice;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Turns a clip of your voice into words with whisper.cpp, running on this computer. Nothing is
 * sent anywhere. If whisper isn't installed, the villager still knows you talked — it just can't
 * make out the words.
 */
public final class Transcriber {

	private static final AtomicBoolean BUSY = new AtomicBoolean();
	private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
		Thread thread = new Thread(task, "Cell Phone whisper");
		thread.setDaemon(true);
		return thread;
	});

	private static final String[] USUAL_WHISPER_PLACES = {
			"/opt/homebrew/bin/whisper-cli",
			"/usr/local/bin/whisper-cli",
			"/usr/bin/whisper-cli",
	};

	private Transcriber() {
	}

	/** True while a clip is being turned into words. One at a time. */
	public static boolean busy() {
		return BUSY.get();
	}

	/**
	 * Works out the words in a clip, then calls done on a background thread with them. done gets
	 * null if whisper isn't available, and "" if there were no words in it (a cough, a door).
	 */
	public static void transcribe(short[] clip, String whisperSetting, String modelSetting,
	                              Path modelFolder, Consumer<String> done) {
		if (!BUSY.compareAndSet(false, true)) {
			return;
		}
		WORKER.execute(() -> {
			try {
				done.accept(run(clip, whisperSetting, modelSetting, modelFolder));
			} finally {
				BUSY.set(false);
			}
		});
	}

	private static String run(short[] clip, String whisperSetting, String modelSetting, Path modelFolder) {
		Path whisper = findWhisper(whisperSetting);
		Path model = findModel(modelSetting, modelFolder);
		if (whisper == null || model == null) {
			return null;
		}
		Path wav = null;
		try {
			wav = Files.createTempFile("cell_phone", ".wav");
			Files.write(wav, wav(clip));
			Process process = new ProcessBuilder(whisper.toString(), "-m", model.toString(),
					"-f", wav.toString(), "-nt", "-np", "-l", "en")
					.redirectError(ProcessBuilder.Redirect.DISCARD)
					.start();
			String out;
			try (InputStream in = process.getInputStream()) {
				out = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
			if (!process.waitFor(30, TimeUnit.SECONDS)) {
				process.destroyForcibly();
				return "";
			}
			return clean(out);
		} catch (IOException e) {
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return "";
		} finally {
			if (wav != null) {
				try {
					Files.deleteIfExists(wav);
				} catch (IOException ignored) {
				}
			}
		}
	}

	/** Whisper writes sounds it can't read as [BLANK_AUDIO], (coughs), *music* and so on. */
	static String clean(String out) {
		String words = out.replaceAll("\\[[^\\]]*\\]", " ")
				.replaceAll("\\([^)]*\\)", " ")
				.replaceAll("\\*[^*]*\\*", " ")
				.replaceAll("\\s+", " ")
				.trim();
		return words.matches(".*[A-Za-z0-9].*") ? words : "";
	}

	private static Path findWhisper(String setting) {
		if (!setting.isBlank()) {
			Path path = Path.of(setting);
			return Files.isExecutable(path) ? path : null;
		}
		for (String place : USUAL_WHISPER_PLACES) {
			Path path = Path.of(place);
			if (Files.isExecutable(path)) {
				return path;
			}
		}
		String pathVar = System.getenv("PATH");
		if (pathVar != null) {
			for (String dir : pathVar.split(java.io.File.pathSeparator)) {
				for (String name : new String[]{"whisper-cli", "whisper-cli.exe"}) {
					Path path = Path.of(dir, name);
					if (Files.isExecutable(path)) {
						return path;
					}
				}
			}
		}
		return null;
	}

	/** A whisper.cpp model: the setting if there is one, else any ggml-*.bin in the mod's config folder. */
	private static Path findModel(String setting, Path folder) {
		if (!setting.isBlank()) {
			Path path = Path.of(setting);
			return Files.isRegularFile(path) ? path : null;
		}
		if (!Files.isDirectory(folder)) {
			return null;
		}
		try (Stream<Path> files = Files.list(folder)) {
			return files.filter(p -> {
						String name = p.getFileName().toString();
						return name.startsWith("ggml-") && name.endsWith(".bin") && Files.isRegularFile(p);
					})
					.sorted()
					.findFirst()
					.orElse(null);
		} catch (IOException e) {
			return null;
		}
	}

	/** A plain 16-bit mono WAV file around the samples. */
	private static byte[] wav(short[] samples) {
		int dataBytes = samples.length * 2;
		ByteBuffer out = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN);
		out.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36 + dataBytes);
		out.put("WAVE".getBytes(StandardCharsets.US_ASCII));
		out.put("fmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16)
				.putShort((short) 1)
				.putShort((short) 1)
				.putInt(Microphone.RATE)
				.putInt(Microphone.RATE * 2)
				.putShort((short) 2)
				.putShort((short) 16);
		out.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(dataBytes);
		for (short s : samples) {
			out.putShort(s);
		}
		return out.array();
	}
}
