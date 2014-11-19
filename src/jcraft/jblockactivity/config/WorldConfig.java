package jcraft.jblockactivity.config;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.extradata.ExtraLoggingTypes.BlockMetaType;
import jcraft.jblockactivity.extradata.ExtraLoggingTypes.EntityMetaType;
import jcraft.jblockactivity.extradata.ExtraLoggingTypes.ItemMetaType;

import org.bukkit.configuration.file.YamlConfiguration;

public class WorldConfig {

    private final File configFile;
    private final YamlConfiguration config;

    private final String worldName;
    public String tableName;
    private boolean[] loggingTypes;

    private boolean saveExtraBlockMeta;
    private boolean[] extraBlockMetaTypes;

    private boolean saveExtraItemMeta;
    private boolean[] extraItemMetaTypes;

    private boolean saveExtraEntityMeta;
    private boolean[] extraEntityMetaTypes;

    private Set<Integer> interactBlocks;
    public int limitEntitiesPerChunk;
    public boolean farmlandForCrops;
    public Set<Integer> blockBlacklist, replaceAnyway, loggingCreatures, loggingHangings;

    public WorldConfig(String worldName) {
        this.worldName = worldName;
        configFile = new File(BlockActivity.DATA_FOLDER, "ba-" + worldName + ".yml");
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void genConfig() {
        final Map<String, Object> configDef = new LinkedHashMap<String, Object>();

        configDef.put("tableName", "ba-" + worldName);

        for (LoggingType type : LoggingType.values()) {
            if (type.getId() <= 0) continue;
            configDef.put("logging." + type.name(), type.getDefaultState());
        }

        configDef.put("logExtraData.blockMeta.enable", true);

        for (BlockMetaType type : BlockMetaType.values()) {
            configDef.put("logExtraData.blockMeta." + type.name(), true);
        }

        configDef.put("logExtraData.itemMeta.enable", true);

        for (ItemMetaType type : ItemMetaType.values()) {
            configDef.put("logExtraData.itemMeta." + type.name(), true);
        }

        configDef.put("logExtraData.entityMeta.enable", true);

        for (EntityMetaType type : EntityMetaType.values()) {
            configDef.put("logExtraData.entityMeta." + type.name(), true);
        }

        configDef.put("special.rollback.limitEntitiesPerChunk", 16);
        configDef.put("special.rollback.replaceAnyway", Arrays.asList(8, 9, 10, 11, 51));
        configDef.put("special.rollback.blockBlacklist", Arrays.asList(10, 11, 46, 51));
        configDef.put("special.rollback.farmlandForCrops", true);
        configDef.put("special.logging.interactBlocks",
                Arrays.asList(23, 25, 54, 61, 62, 64, 69, 77, 84, 92, 93, 94, 96, 107, 117, 138, 143, 145, 146, 149, 150, 154, 158));
        configDef.put("special.logging.creatures", Arrays.asList(90, 91, 92, 93, 95, 96, 98, 100, 120));
        configDef.put("special.logging.hangings", Arrays.asList(9, 18));

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

        tableName = config.getString("tableName");

        loggingTypes = new boolean[LoggingType.values().length];

        for (LoggingType type : LoggingType.values()) {
            if (type.getId() <= 0) {
                continue;
            }

            boolean result = config.getBoolean("logging." + type.name());
            loggingTypes[type.ordinal()] = result;
        }

        saveExtraBlockMeta = config.getBoolean("logExtraData.blockMeta.enable");
        extraBlockMetaTypes = new boolean[BlockMetaType.values().length];

        for (BlockMetaType type : BlockMetaType.values()) {
            boolean result = config.getBoolean("logExtraData.blockMeta." + type.name());
            extraBlockMetaTypes[type.ordinal()] = result;
        }

        saveExtraItemMeta = config.getBoolean("logExtraData.itemMeta.enable");
        extraItemMetaTypes = new boolean[ItemMetaType.values().length];

        for (ItemMetaType type : ItemMetaType.values()) {
            boolean result = saveExtraItemMeta ? config.getBoolean("logExtraData.itemMeta." + type.name()) : false;
            extraItemMetaTypes[type.ordinal()] = result;
        }

        saveExtraEntityMeta = config.getBoolean("logExtraData.entityMeta.enable");
        extraEntityMetaTypes = new boolean[EntityMetaType.values().length];

        for (EntityMetaType type : EntityMetaType.values()) {
            boolean result = saveExtraEntityMeta ? config.getBoolean("logExtraData.entityMeta." + type.name()) : false;
            extraEntityMetaTypes[type.ordinal()] = result;
        }

        if (isLogging(LoggingType.blockinteract)) {
            interactBlocks = new HashSet<Integer>(config.getIntegerList("special.logging.interactBlocks"));
        }

        if (isLogging(LoggingType.creaturekill)) {
            loggingCreatures = new HashSet<Integer>(config.getIntegerList("special.logging.creatures"));
        }

        if (isLogging(LoggingType.hangingplace) || isLogging(LoggingType.hangingbreak) || isLogging(LoggingType.hanginginteract)) {
            loggingHangings = new HashSet<Integer>(config.getIntegerList("special.logging.hangings"));
        }

        limitEntitiesPerChunk = config.getInt("special.rollback.limitEntitiesPerChunk");
        farmlandForCrops = config.getBoolean("special.rollback.farmlandForCrops");
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

    public boolean isLoggingExtraBlockMeta(BlockMetaType logType) {
        return extraBlockMetaTypes[logType.ordinal()];
    }

    public boolean isLoggingExtraBlockMeta() {
        return saveExtraBlockMeta;
    }

    public boolean isLoggingExtraItemMeta(ItemMetaType type) {
        return extraItemMetaTypes[type.ordinal()];
    }

    public boolean isLoggingExtraItemMeta() {
        return saveExtraItemMeta;
    }

    public boolean isLoggingExtraEntityMeta(EntityMetaType type) {
        return extraEntityMetaTypes[type.ordinal()];
    }

    public boolean isLoggingExtraEntityMeta() {
        return saveExtraEntityMeta;
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

        state.executeUpdate("CREATE TABLE IF NOT EXISTS `"
                + tableName
                + "` (id INT UNSIGNED NOT NULL AUTO_INCREMENT, time DATETIME NOT NULL, type SMALLINT UNSIGNED NOT NULL, playerid MEDIUMINT UNSIGNED NOT NULL, old_id MEDIUMINT UNSIGNED NOT NULL, old_data MEDIUMINT UNSIGNED NOT NULL, new_id MEDIUMINT UNSIGNED NOT NULL, new_data MEDIUMINT UNSIGNED NOT NULL, x MEDIUMINT NOT NULL, y SMALLINT UNSIGNED NOT NULL, z MEDIUMINT NOT NULL, PRIMARY KEY (id), KEY coords (x, z, y), KEY time (time), KEY playerid (playerid))");

        if (isLoggingExtraBlockMeta()) {
            state.executeUpdate("CREATE TABLE IF NOT EXISTS `" + tableName
                    + "-extra` (id INT UNSIGNED NOT NULL, data text, PRIMARY KEY (id)) DEFAULT CHARSET=utf8");
        }

        state.close();
        connection.close();
    }

}
