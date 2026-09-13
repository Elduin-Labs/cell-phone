package com.example.modtemplate;

import com.example.modtemplate.platform.Platform;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.modtemplate.platform.fabric.FabricPlatform;

@SuppressWarnings("LoggingSimilarMessage")
public class ModTemplate {

	public static final String MOD_ID = /*$ mod_id*/ "modtemplate";
	public static final String MOD_VERSION = /*$ mod_version*/ "1.0.0";
	public static final String MOD_FRIENDLY_NAME = /*$ mod_name*/ "Mod Template";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final Platform PLATFORM = createPlatformInstance();

	public static void onInitialize() {
		LOGGER.info("Initializing {} on {}", MOD_ID, ModTemplate.xplat().loader());
		LOGGER.debug("{}: { version: {}; friendly_name: {} }", MOD_ID, MOD_VERSION, MOD_FRIENDLY_NAME);
	}

	public static void onInitializeClient() {
		LOGGER.info("Initializing {} Client on {}", MOD_ID, ModTemplate.xplat().loader());
		LOGGER.debug("{}: { version: {}; friendly_name: {} }", MOD_ID, MOD_VERSION, MOD_FRIENDLY_NAME);
	}

	static Platform xplat() {
		return PLATFORM;
	}

	private static Platform createPlatformInstance() {
		return new FabricPlatform();
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	private static Identifier id(String namespace, String path) {
		return Identifier.fromNamespaceAndPath(namespace, path);
	}
}
