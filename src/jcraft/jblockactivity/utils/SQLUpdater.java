package jcraft.jblockactivity.utils;

import java.io.IOException;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.config.ActivityConfig;

import org.bukkit.configuration.file.YamlConfiguration;

public class SQLUpdater implements Runnable {

    private final int LOADED_VERSION;

    public SQLUpdater(int version) {
        LOADED_VERSION = version;
    }

    @Override
    public void run() {
        /**
         * @note Do future config/table updates here
         */

        updateVersion();
    }

    private void updateVersion() {
        if (LOADED_VERSION > BlockActivity.LATEST_VERSION) {
            final YamlConfiguration config = YamlConfiguration.loadConfiguration(ActivityConfig.CONFIG_FILE);
            config.set("configVersion", BlockActivity.LATEST_VERSION);
            try {
                config.save(ActivityConfig.CONFIG_FILE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
