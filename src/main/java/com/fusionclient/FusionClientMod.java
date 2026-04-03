package com.fusionclient;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FusionClientMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("fusionclient");

    @Override
    public void onInitialize() {
        LOGGER.info("Fusion Client initializing...");
    }
}
