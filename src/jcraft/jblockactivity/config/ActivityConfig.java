package jcraft.jblockactivity.config;

import static jcraft.jblockactivity.utils.ActivityUtil.parseTime;
import static org.bukkit.Bukkit.getConsoleSender;
import static org.bukkit.Bukkit.getWorld;
import static org.bukkit.Bukkit.getWorlds;

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
import jcraft.jblockactivity.utils.QueryParams;

import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

public class ActivityConfig {

    private final File configFile;
    private final YamlConfiguration config;

    public ActivityConfig() {
        configFile = new File(BlockActivity.dataFolder, "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public String sqlHost, sqlPort, sqlDatabase, sqlUsername, sqlPassword;
    public int maxTimePerRun, timeBetweenRuns, minLogsToProcess, queueWarningSize;
    private boolean[] loggingTypes;

    public int defaultDistance, defaultTime;
    public int linesPerPage;
    public long toolUseCooldown;

    public Set<String> hiddenPlayers;
    public boolean askRollbacks, askRedos, askClearlogs;
    public boolean callActionLogQueueEvent;

    public void genConfig() {
        final Map<String, Object> configDef = new LinkedHashMap<String, Object>();

        configDef.put("configVersion", 1);

        configDef.put("mysql.host", "127.0.0.1");
        configDef.put("mysql.port", 3306);
        configDef.put("mysql.username", "root");
        configDef.put("mysql.password", "password");
        configDef.put("mysql.database", "minecraft");

        final List<String> logWorlds = new ArrayList<String>();
        for (World world : getWorlds()) {
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
        configDef.put("tools.itemtool.params", "area 0 all sum none limit 5 desc");
        configDef.put("tools.itemtool.leftClickBehavior", "TOOL");
        configDef.put("tools.itemtool.rightClickBehavior", "BLOCK");

        configDef.put("tools.blocktool.itemId", 7);
        configDef.put("tools.blocktool.itemData", 0);
        configDef.put("tools.blocktool.params", "area 0 all sum none limit 15 desc");
        configDef.put("tools.blocktool.leftClickBehavior", "TOOL");
        configDef.put("tools.blocktool.rightClickBehavior", "BLOCK");

        configDef.put("questioner.askRollbacks", true);
        configDef.put("questioner.askRedos", true);
        configDef.put("questioner.askClearlogs", true);

        configDef.put("event.callActionLogQueueEvent", false);

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

    public void loadConfig() {
        genConfig();

        sqlHost = getStringIncludingInts("mysql.host");
        sqlPort = getStringIncludingInts("mysql.port");
        sqlDatabase = config.getString("mysql.database");
        sqlUsername = getStringIncludingInts("mysql.username");
        sqlPassword = getStringIncludingInts("mysql.password");
        BlockActivity.sqlProfile = new SQLProfile(sqlHost, sqlPort, sqlDatabase, sqlUsername, sqlPassword);

        maxTimePerRun = config.getInt("queue.maxTimePerRun");
        timeBetweenRuns = config.getInt("queue.timeBetweenRuns");
        minLogsToProcess = config.getInt("queue.minLogsToProcess");
        queueWarningSize = config.getInt("queue.queueWarningSize");

        defaultDistance = config.getInt("lookup.defaultDistance");
        defaultTime = parseTime(config.getString("lookup.defaultTime").split(" "));

        linesPerPage = config.getInt("lookup.linesPerPage");

        toolUseCooldown = config.getLong("tools.useCooldown");

        final int itemId = config.getInt("tools.itemtool.itemId");
        if (itemId != 0) {
            final byte itemData = (byte) config.getInt("tools.itemtool.itemData");
            final QueryParams params = new QueryParams(getConsoleSender(), config.getString("tools.itemtool.params").split(" "), true);
            final ToolBehavior leftClickBehavior = ToolBehavior.valueOf(config.getString("tools.itemtool.leftClickBehavior").toUpperCase());
            final ToolBehavior rightClickBehavior = ToolBehavior.valueOf(config.getString("tools.itemtool.rightClickBehavior").toUpperCase());
            final LogTool itemTool = new LogTool(leftClickBehavior, rightClickBehavior, itemId, itemData, params);
            BlockActivity.logItemTool = itemTool;
        }

        final int blockId = config.getInt("tools.blocktool.itemId");
        if (blockId != 0) {
            final byte itemData = (byte) config.getInt("tools.blocktool.itemData");
            final QueryParams params = new QueryParams(getConsoleSender(), config.getString("tools.blocktool.params").split(" "), true);
            final ToolBehavior leftClickBehavior = ToolBehavior.valueOf(config.getString("tools.blocktool.leftClickBehavior").toUpperCase());
            final ToolBehavior rightClickBehavior = ToolBehavior.valueOf(config.getString("tools.blocktool.rightClickBehavior").toUpperCase());
            final LogTool blockTool = new LogTool(leftClickBehavior, rightClickBehavior, blockId, itemData, params);
            BlockActivity.logBlockTool = blockTool;
        }

        hiddenPlayers = new HashSet<String>();
        for (final String playerName : config.getStringList("logging.hiddenPlayers")) {
            hiddenPlayers.add(playerName.trim());
        }

        askRollbacks = config.getBoolean("questioner.askRollbacks");
        askRedos = config.getBoolean("questioner.askRedos");
        askClearlogs = config.getBoolean("questioner.askClearlogs");

        callActionLogQueueEvent = config.getBoolean("event.callActionLogQueueEvent");
    }

    public void loadWorldConfig() {
        final List<String> sWorlds = config.getStringList("loggedWorlds");
        for (String worldName : sWorlds) {
            World world = getWorld(worldName);
            if (world != null) {
                WorldConfig worldConfig = new WorldConfig(world);
                if (worldConfig.loadConfig()) {
                    BlockActivity.worldConfigs.put(world.getName(), worldConfig);
                }
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
        String str = config.getString(key);
        if (str == null) {
            str = String.valueOf(config.getInt(key));
        }
        return str;
    }
}
