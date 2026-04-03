package com.fusionclient.client;

import com.fusionclient.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FusionClientClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("fusionclient");

    @Override
    public void initializeClient() {
        LOGGER.info("Fusion Client initializing client...");
        ModuleManager.getInstance().registerModules();
    }
}
