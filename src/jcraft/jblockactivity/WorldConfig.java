package jcraft.jblockactivity;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

public class WorldConfig {

    private final File configFile;
    private final YamlConfiguration config;

    private final String worldName;
    private boolean[] loggingTypes;
    private boolean[] extraLoggingTypes;
    private Set<Integer> interactBlocks;
    public int limitEntitiesPerChunk;
    public Set<Integer> blockBlacklist, replaceAnyway, loggingCreatures;

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

        configDef.put("special.rollback.LimitEntitiesPerChunk", 16);
        configDef.put("special.rollback.replaceAnyway", Arrays.asList(8, 9, 10, 11, 51));
        configDef.put("special.rollback.blockBlacklist", Arrays.asList(10, 11, 46, 51));
        configDef.put("special.logging.interactBlocks",
                Arrays.asList(23, 25, 54, 61, 62, 64, 69, 77, 84, 92, 93, 94, 96, 107, 117, 138, 143, 145, 146, 149, 150, 154, 158));
        configDef.put("special.logging.creatures", Arrays.asList(90, 91, 92, 93, 95, 96, 98, 100, 120));

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

        if (isLogging(LoggingType.blockinteract)) {
            interactBlocks = new HashSet<Integer>(config.getIntegerList("special.logging.interactBlocks"));
        }
        if (isLogging(LoggingType.creaturekill)) {
            loggingCreatures = new HashSet<Integer>(config.getIntegerList("special.logging.creatures"));
        }

        limitEntitiesPerChunk = config.getInt("special.rollback.LimitEntitiesPerChunk");
        replaceAnyway = new HashSet<Integer>(config.getIntegerList("special.rollback.replaceAnyway"));
        blockBlacklist = new HashSet<Integer>(config.getIntegerList("special.rollback.blockBlacklist"));

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

    public boolean isInteractiveBlock(int id) {
        return interactBlocks.contains(id);
    }

    public void createTable() throws SQLException {
        final Connection connection = BlockActivity.getBlockActivity().getConnection();
        if (connection == null) {
            return;
        }
        final Statement state = connection.createStatement();
        state.executeUpdate("CREATE TABLE IF NOT EXISTS `ba-"
                + worldName
                + "` (id INT UNSIGNED NOT NULL AUTO_INCREMENT, time DATETIME NOT NULL, type INT UNSIGNED NOT NULL, playerid INT UNSIGNED NOT NULL, old_id INT UNSIGNED NOT NULL, old_data INT UNSIGNED NOT NULL, new_id INT UNSIGNED NOT NULL, new_data INT UNSIGNED NOT NULL, x MEDIUMINT NOT NULL, y SMALLINT UNSIGNED NOT NULL, z MEDIUMINT NOT NULL, PRIMARY KEY (id), KEY coords (x, z, y), KEY time (time), KEY playerid (playerid))");
        if (isExtraLogging()) {
            state.executeUpdate("CREATE TABLE IF NOT EXISTS `ba-" + worldName
                    + "-extra` (id INT UNSIGNED NOT NULL, data text, PRIMARY KEY (id)) DEFAULT CHARSET=utf8");
        }
        state.close();
        connection.close();
    }

}
