package com.elduin.cell_phone.client;

import com.elduin.cell_phone.CellPhone;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** config/cell_phone.properties — the things a player can change. Written with defaults on first run. */
final class PhoneConfig {

	private static final String HEADER = """
			 Cell Phone settings.

			 microphone   true lets you talk to villagers out loud during a call.
			 sensitivity  how easily the phone notices you talking. Bigger hears quieter voices.
			 whisper      path to whisper-cli (whisper.cpp), which turns your voice into words.
			              Leave blank to look in the usual places.
			 model        path to a whisper.cpp model (a ggml-*.bin file). Leave blank to use
			              the first one in config/cell_phone/.""";

	final boolean microphone;
	final float sensitivity;
	final String whisper;
	final String model;

	private PhoneConfig(Properties p) {
		microphone = Boolean.parseBoolean(p.getProperty("microphone", "true").trim());
		sensitivity = parseFloat(p.getProperty("sensitivity", "1.0"), 1.0f);
		whisper = p.getProperty("whisper", "").trim();
		model = p.getProperty("model", "").trim();
	}

	/** Where a whisper model can be dropped in without touching the settings. */
	static Path modelFolder() {
		return FabricLoader.getInstance().getConfigDir().resolve(CellPhone.MOD_ID);
	}

	static PhoneConfig load() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve(CellPhone.MOD_ID + ".properties");
		Properties p = new Properties();
		if (Files.exists(file)) {
			try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				p.load(in);
			} catch (IOException e) {
				CellPhone.LOGGER.warn("Could not read {}, using defaults", file, e);
			}
		}
		PhoneConfig config = new PhoneConfig(p);
		config.save(file);
		return config;
	}

	private void save(Path file) {
		Properties p = new Properties();
		p.setProperty("microphone", Boolean.toString(microphone));
		p.setProperty("sensitivity", Float.toString(sensitivity));
		p.setProperty("whisper", whisper);
		p.setProperty("model", model);
		try {
			Files.createDirectories(file.getParent());
			try (Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				p.store(out, HEADER);
			}
		} catch (IOException e) {
			CellPhone.LOGGER.warn("Could not write {}", file, e);
		}
	}

	private static float parseFloat(String text, float fallback) {
		try {
			return Float.parseFloat(text.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
