package jcraft.jblockactivity;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import jcraft.jblockactivity.actionlogs.ActionLog;
import jcraft.jblockactivity.listeners.BlockBreakListener;
import jcraft.jblockactivity.listeners.BlockInteractListener;
import jcraft.jblockactivity.listeners.BlockPlaceListener;
import jcraft.jblockactivity.listeners.CreatureKillListener;
import jcraft.jblockactivity.listeners.HangingListener;
import jcraft.jblockactivity.listeners.InventoryAccessListener;
import jcraft.jblockactivity.listeners.LogToolListener;
import jcraft.jblockactivity.sql.SQLConnectionPool;
import jcraft.jblockactivity.sql.SQLProfile;
import jcraft.jblockactivity.tool.LogTool;

import org.bukkit.ChatColor;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BlockActivity extends JavaPlugin {

    public static File dataFolder;
    public final static String prefix = ChatColor.GOLD + "[" + ChatColor.WHITE + "jBlockActivity" + ChatColor.GOLD + "] " + ChatColor.WHITE;

    private static BlockActivity blockActivity;
    private static CommandHandler cmdHandler;

    public static ActivityConfig config;

    public static SQLProfile sqlProfile;
    private static SQLConnectionPool connectionPool;
    private static boolean errorAtLoading = false, connected = true;

    private static Thread logExecuteThread;
    private static LogExecuteThread logExecuteRunnable;
    private static Thread actionExecuteThread;
    private static ActionExecuteThread actionExecuteRunnable;

    public final static Map<String, WorldConfig> worldConfigs = new HashMap<String, WorldConfig>();
    public final static Map<String, Integer> playerIds = new HashMap<String, Integer>();

    public static LogTool logItemTool;
    public static LogTool logBlockTool;
    public final static Map<String, Long> lastToolUse = new HashMap<String, Long>();

    public static BlockActivity getBlockActivity() {
        return blockActivity;
    }

    public static CommandHandler getCommandHandler() {
        return cmdHandler;
    }

    public void onLoad() {
        dataFolder = getDataFolder();
        blockActivity = this;

        config = new ActivityConfig();
        config.loadConfig();

        try {
            connectionPool = new SQLConnectionPool(sqlProfile);
            final Connection connection = getConnection();
            if (connection == null) {
                errorAtLoading = true;
                return;
            }
            createTables();
            connection.close();
        } catch (final NullPointerException ex) {
            getLogger().log(Level.SEVERE, "Error while loading: ", ex);
        } catch (final Exception ex) {
            getLogger().severe("Error while loading: " + ex.getMessage());
            errorAtLoading = true;
            return;
        }
    }

    public void onEnable() {
        if (errorAtLoading) {
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        config.loadWorldConfig();

        cmdHandler = new CommandHandler();
        getCommand("ba").setExecutor(cmdHandler);

        logExecuteRunnable = new LogExecuteThread(config.maxTimePerRun, config.timeBetweenRuns, config.minLogsToProcess, config.queueWarningSize);
        logExecuteThread = new Thread(logExecuteRunnable, "jBlockLogExecutor");
        logExecuteThread.start();

        actionExecuteRunnable = new ActionExecuteThread(cmdHandler);
        actionExecuteThread = new Thread(actionExecuteRunnable, "jBlockActionExecutor");
        actionExecuteThread.start();

        final PluginManager manager = getServer().getPluginManager();
        if (config.isWorldsLogging(LoggingType.blockplace)) {
            manager.registerEvents(new BlockPlaceListener(), this);
        }
        if (config.isWorldsLogging(LoggingType.blockbreak)) {
            manager.registerEvents(new BlockBreakListener(), this);
        }
        if (config.isWorldsLogging(LoggingType.inventoryaccess)) {
            manager.registerEvents(new InventoryAccessListener(), this);
        }
        if (config.isWorldsLogging(LoggingType.blockinteract)) {
            manager.registerEvents(new BlockInteractListener(), this);
        }
        if (config.isWorldsLogging(LoggingType.hangingbreak) || config.isWorldsLogging(LoggingType.hangingplace)
                || config.isWorldsLogging(LoggingType.hanginginteract)) {
            manager.registerEvents(new HangingListener(), this);
        }
        if (config.isWorldsLogging(LoggingType.creaturekill)) {
            manager.registerEvents(new CreatureKillListener(), this);
        }
        manager.registerEvents(new LogToolListener(), this);
    }

    public void onDisable() {
        if (logExecuteRunnable != null && logExecuteThread != null) {
            try {
                getLogger().log(Level.INFO, "Sending remaining logs(" + logExecuteRunnable.getQueueSize() + ")...");
                logExecuteRunnable.terminate();
                logExecuteThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (connectionPool != null) {
            connectionPool.close();
        }
    }

    public Connection getConnection() {
        try {
            final Connection connection = connectionPool.getConnection();
            if (!connected) {
                getLogger().info("MySQL connection rebuild...");
                connected = true;
            }
            return connection;
        } catch (final Exception ex) {
            if (connected) {
                getLogger().log(Level.SEVERE, "Error while fetching connection: ", ex);
                connected = false;
            } else {
                getLogger().severe("MySQL connection lost!");
            }
            return null;
        }
    }

    public void createTables() throws SQLException {
        final Connection connection = getConnection();
        if (connection == null) {
            throw new SQLException("No connection!");
        }
        final Statement state = connection.createStatement();
        state.executeUpdate("CREATE TABLE IF NOT EXISTS `ba-players` (playerid INT UNSIGNED NOT NULL AUTO_INCREMENT, playername varchar(32) NOT NULL, primary key (playerid))");
        state.close();
        connection.close();
    }

    public static void sendActionLog(ActionLog... actions) {
        if (actions == null) {
            return;
        }
        for (ActionLog action : actions) {
            if (action == null) {
                continue;
            }
            logExecuteRunnable.queue.add(action);
        }
    }

    public static LogExecuteThread getLogExecuteThread() {
        return logExecuteRunnable;
    }

    public static WorldConfig getWorldConfig(String name) {
        if (worldConfigs.containsKey(name)) {
            return worldConfigs.get(name);
        }
        return null;
    }

    public static String getWorldTableName(String name) {
        final WorldConfig config = getWorldConfig(name);
        if (config != null) {
            return config.tableName;
        }
        return null;
    }

    public static boolean isWorldLogged(String name) {
        if (worldConfigs.containsKey(name)) {
            return true;
        }
        return false;
    }

    public static LogTool getLogItem(MaterialData data) {
        if (logItemTool != null) {
            final MaterialData material = logItemTool.itemMaterial;
            if (material.getItemTypeId() == data.getItemTypeId() && material.getData() == data.getData()) {
                return logItemTool;
            }
        }
        if (logBlockTool != null) {
            final MaterialData material = logBlockTool.itemMaterial;
            if (material.getItemTypeId() == data.getItemTypeId() && material.getData() == data.getData()) {
                return logBlockTool;
            }
        }
        return null;
    }

    public static boolean isHidden(String playerName) {
        return config.hiddenPlayers.contains(playerName);
    }

    public static boolean hidePlayer(String playerName) {
        if (config.hiddenPlayers.contains(playerName)) {
            config.hiddenPlayers.remove(playerName);
            return false;
        }
        config.hiddenPlayers.add(playerName);
        return true;
    }

}
