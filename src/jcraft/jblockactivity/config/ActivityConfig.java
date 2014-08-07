package jcraft.jblockactivity.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.sql.SQLProfile;
import jcraft.jblockactivity.tool.LogTool;
import jcraft.jblockactivity.tool.LogTool.ToolBehavior;
import jcraft.jblockactivity.utils.ActivityUtil;
import jcraft.jblockactivity.utils.QueryParams;
import jcraft.jblockactivity.utils.SQLUpdater;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

public class ActivityConfig {

    public static File CONFIG_FILE;
    public static YamlConfiguration CONFIG;

    public ActivityConfig() {
        CONFIG_FILE = new File(BlockActivity.dataFolder, "config.yml");
        CONFIG = YamlConfiguration.loadConfiguration(CONFIG_FILE);
    }

    public String sqlHost, sqlPort, sqlDatabase, sqlUsername, sqlPassword;
    public int connectionsPoolSize, connectionLifeSpan;
    public int maxTimePerRun, timeBetweenRuns, minLogsToProcess, queueWarningSize;
    private boolean[] loggingTypes;

    public int defaultDistance, defaultTime;
    public int linesPerPage;
    public long toolUseCooldown;

    public Set<String> hiddenPlayers;
    public boolean askRollbacks, askRedos, askClearlogs;
    public boolean callActionLogQueueEvent;
    public boolean forceWorldEditPlugin;

    public void genConfig() {
        final Map<String, Object> configDef = new LinkedHashMap<String, Object>();

        configDef.put("configVersion", BlockActivity.LATEST_VERSION);
        configDef.put("autoUpdateTables", true);

        configDef.put("mysql.host", "127.0.0.1");
        configDef.put("mysql.port", 3306);
        configDef.put("mysql.username", "root");
        configDef.put("mysql.password", "password");
        configDef.put("mysql.database", "minecraft");

        configDef.put("mysql.connection.poolSize", 10);
        configDef.put("mysql.connection.lifeSpan", 300000);

        final List<String> logWorlds = new ArrayList<String>();
        for (World world : Bukkit.getWorlds()) {
            logWorlds.add(world.getName());
        }
        configDef.put("loggedWorlds", logWorlds);

        configDef.put("logging.hiddenPlayers", Arrays.asList("Dinnerbone"));

        configDef.put("queue.maxTimePerRun", 300);
        configDef.put("queue.timeBetweenRuns", 5);
        configDef.put("queue.minLogsToProcess", 25);
        configDef.put("queue.queueWarningSize", 1000);

        configDef.put("lookup.defaultDistance", 20);
        configDef.put("lookup.defaultTime", "30 minutes");
        configDef.put("lookup.linesPerPage", 15);

        configDef.put("tools.useCooldown", 3000);

        configDef.put("tools.itemtool.itemId", 294);
        configDef.put("tools.itemtool.itemData", 0);
        configDef.put("tools.itemtool.params", "area 0 all sum none limit 5 extra desc");
        configDef.put("tools.itemtool.leftClickBehavior", "TOOL");
        configDef.put("tools.itemtool.rightClickBehavior", "BLOCK");

        configDef.put("tools.blocktool.itemId", 7);
        configDef.put("tools.blocktool.itemData", 0);
        configDef.put("tools.blocktool.params", "area 0 all sum none limit 15 extra desc");
        configDef.put("tools.blocktool.leftClickBehavior", "TOOL");
        configDef.put("tools.blocktool.rightClickBehavior", "BLOCK");

        configDef.put("questioner.askRollbacks", true);
        configDef.put("questioner.askRedos", true);
        configDef.put("questioner.askClearlogs", true);

        configDef.put("event.callActionLogQueueEvent", false);

        configDef.put("plugins.forceWorldEditPlugin", false);

        for (Entry<String, Object> e : configDef.entrySet()) {
            if (!CONFIG.contains(e.getKey())) {
                CONFIG.set(e.getKey(), e.getValue());
            }
        }

        try {
            CONFIG.save(CONFIG_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadConfig() {
        genConfig();

        sqlHost = getStringIncludingInts("mysql.host");
        sqlPort = getStringIncludingInts("mysql.port");
        sqlDatabase = CONFIG.getString("mysql.database");
        sqlUsername = getStringIncludingInts("mysql.username");
        sqlPassword = getStringIncludingInts("mysql.password");
        BlockActivity.sqlProfile = new SQLProfile(sqlHost, sqlPort, sqlDatabase, sqlUsername, sqlPassword);

        connectionsPoolSize = CONFIG.getInt("mysql.connection.poolSize");
        connectionLifeSpan = CONFIG.getInt("mysql.connection.lifeSpan");

        maxTimePerRun = CONFIG.getInt("queue.maxTimePerRun");
        timeBetweenRuns = CONFIG.getInt("queue.timeBetweenRuns");
        minLogsToProcess = CONFIG.getInt("queue.minLogsToProcess");
        queueWarningSize = CONFIG.getInt("queue.queueWarningSize");

        defaultDistance = CONFIG.getInt("lookup.defaultDistance");
        defaultTime = ActivityUtil.parseTime(CONFIG.getString("lookup.defaultTime").split(" "));

        linesPerPage = CONFIG.getInt("lookup.linesPerPage");

        toolUseCooldown = CONFIG.getLong("tools.useCooldown");

        final int itemId = CONFIG.getInt("tools.itemtool.itemId");
        if (itemId != 0) {
            final byte itemData = (byte) CONFIG.getInt("tools.itemtool.itemData");
            final QueryParams params = new QueryParams(Bukkit.getConsoleSender(), CONFIG.getString("tools.itemtool.params").split(" "), true);
            final ToolBehavior leftClickBehavior = ToolBehavior.valueOf(CONFIG.getString("tools.itemtool.leftClickBehavior").toUpperCase());
            final ToolBehavior rightClickBehavior = ToolBehavior.valueOf(CONFIG.getString("tools.itemtool.rightClickBehavior").toUpperCase());
            final LogTool itemTool = new LogTool(leftClickBehavior, rightClickBehavior, itemId, itemData, params);
            BlockActivity.logItemTool = itemTool;
        }

        final int blockId = CONFIG.getInt("tools.blocktool.itemId");
        if (blockId != 0) {
            final byte itemData = (byte) CONFIG.getInt("tools.blocktool.itemData");
            final QueryParams params = new QueryParams(Bukkit.getConsoleSender(), CONFIG.getString("tools.blocktool.params").split(" "), true);
            final ToolBehavior leftClickBehavior = ToolBehavior.valueOf(CONFIG.getString("tools.blocktool.leftClickBehavior").toUpperCase());
            final ToolBehavior rightClickBehavior = ToolBehavior.valueOf(CONFIG.getString("tools.blocktool.rightClickBehavior").toUpperCase());
            final LogTool blockTool = new LogTool(leftClickBehavior, rightClickBehavior, blockId, itemData, params);
            BlockActivity.logBlockTool = blockTool;
        }

        hiddenPlayers = new HashSet<String>();
        for (final String name : CONFIG.getStringList("logging.hiddenPlayers")) {
            hiddenPlayers.add(name.trim());
        }

        askRollbacks = CONFIG.getBoolean("questioner.askRollbacks");
        askRedos = CONFIG.getBoolean("questioner.askRedos");
        askClearlogs = CONFIG.getBoolean("questioner.askClearlogs");

        callActionLogQueueEvent = CONFIG.getBoolean("event.callActionLogQueueEvent");

        forceWorldEditPlugin = CONFIG.getBoolean("plugins.forceWorldEditPlugin");
    }

    public void checkConfigVersion() {
        if (CONFIG.getBoolean("autoUpdateTables")) {
            final int version = CONFIG.getInt("configVersion");
            new Thread(new SQLUpdater(version), "jBA-Updater").start();
        }
    }

    public void loadWorldConfig() {
        final List<String> sWorlds = CONFIG.getStringList("loggedWorlds");
        for (String worldName : sWorlds) {
            WorldConfig worldConfig = new WorldConfig(worldName);
            if (worldConfig.loadConfig()) {
                BlockActivity.worldConfigs.put(worldName, worldConfig);
            }
        }

        loggingTypes = new boolean[LoggingType.values().length];
        for (WorldConfig worldConfig : BlockActivity.worldConfigs.values()) {
            for (LoggingType type : LoggingType.values()) {
                if (worldConfig.isLogging(type)) {
                    loggingTypes[type.ordinal()] = true;
                }
            }

        }
    }

    public boolean isWorldsLogging(LoggingType logType) {
        return loggingTypes[logType.ordinal()];
    }

    private String getStringIncludingInts(String key) {
        String str = CONFIG.getString(key);
        if (str == null) {
            str = String.valueOf(CONFIG.getInt(key));
        }
        return str;
    }

}
