package jcraft.jblockactivity.utils;

import java.io.IOException;

import jcraft.jblockactivity.config.ActivityConfig;

import org.bukkit.configuration.file.YamlConfiguration;

public class SQLUpdater {

    private final static int LATEST_VERSION = 1;

    public SQLUpdater(int version) {
        if (version > LATEST_VERSION) {
            final YamlConfiguration config = YamlConfiguration.loadConfiguration(ActivityConfig.CONFIG_FILE);
            config.set("configVersion", LATEST_VERSION);
            try {
                config.save(ActivityConfig.CONFIG_FILE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * @note Do future config/table updates here
         */
    }

}
