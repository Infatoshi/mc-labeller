package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.network.ActionReceiver;

public class ExampleMod implements ModInitializer {
	public static final String MOD_ID = "modid";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Starting Python control interface...");
		ActionReceiver.startListener();

		// Register shutdown hook
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			LOGGER.info("Shutting down Python control interface...");
			ActionReceiver.shutdown();
		});
	}
}