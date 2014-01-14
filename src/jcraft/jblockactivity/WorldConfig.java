package jcraft.jblockactivity;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import jcraft.jblockactivity.sql.SQLConnection;

import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

public class WorldConfig {

    private final String worldName;
    private boolean[] loggingTypes;
    private boolean[] extraLoggingTypes;

    private final File configFile;
    private final YamlConfiguration config;

    public WorldConfig(World world) {
        this.worldName = world.getName().toLowerCase();
        configFile = new File(BlockActivity.dataFolder, "ba-" + worldName + ".yml");
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void genConfig() {
        final Map<String, Object> configDef = new HashMap<String, Object>();

        for (LoggingType type : LoggingType.values()) {
            if (type.getId() <= 0) continue;
            configDef.put("logging." + type.name(), true);
        }
        for (ExtraLoggingType type : ExtraLoggingType.values()) {
            if (type.getId() <= 0) continue;
            configDef.put("logExtraData." + type.name(), true);
        }

        for (Entry<String, Object> e : configDef.entrySet()) {
            if (!config.contains(e.getKey())) {
                config.set(e.getKey(), e.getValue());
            }
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean loadConfig() {
        genConfig();

        loggingTypes = new boolean[LoggingType.values().length];
        for (LoggingType type : LoggingType.values()) {
            if (type.getId() <= 0) continue;
            boolean result = config.getBoolean("logging." + type.name());
            loggingTypes[type.ordinal()] = result;
        }
        extraLoggingTypes = new boolean[ExtraLoggingType.values().length];
        for (ExtraLoggingType type : ExtraLoggingType.values()) {
            if (type.getId() <= 0) continue;
            boolean result = config.getBoolean("logExtraData." + type.name());
            extraLoggingTypes[type.ordinal()] = result;
        }

        try {
            createTable();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean isLogging(LoggingType logType) {
        return loggingTypes[logType.ordinal()];
    }

    public boolean isExtraLogging(ExtraLoggingType logType) {
        return extraLoggingTypes[logType.ordinal()];
    }

    public boolean isExtraLogging() {
        for (boolean state : extraLoggingTypes) {
            if (state) {
                return true;
            }
        }
        return false;
    }

    public void createTable() throws SQLException {
        final SQLConnection sqlConnection = new SQLConnection(BlockActivity.sqlProfile);
        sqlConnection.open();
        final Statement state = sqlConnection.getConnection().createStatement();
        state.executeUpdate("CREATE TABLE IF NOT EXISTS `ba-"
                + worldName
                + "` (id INT UNSIGNED NOT NULL AUTO_INCREMENT, time DATETIME NOT NULL, type INT UNSIGNED NOT NULL, playerid INT UNSIGNED NOT NULL, old_id INT UNSIGNED NOT NULL, old_data INT UNSIGNED NOT NULL, new_id INT UNSIGNED NOT NULL, new_data INT UNSIGNED NOT NULL, x MEDIUMINT NOT NULL, y SMALLINT UNSIGNED NOT NULL, z MEDIUMINT NOT NULL, PRIMARY KEY (id), KEY coords (x, z, y), KEY time (time), KEY playerid (playerid))");
        if (isExtraLogging()) {
            state.executeUpdate("CREATE TABLE IF NOT EXISTS `ba-" + worldName
                    + "-extra` (id INT UNSIGNED NOT NULL, data text, PRIMARY KEY (id)) DEFAULT CHARSET=utf8");
        }
        state.close();
        sqlConnection.closeConnection();
    }

}
