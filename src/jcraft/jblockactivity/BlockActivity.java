package jcraft.jblockactivity;

import java.io.File;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import jcraft.jblockactivity.actionlogs.ActionLog;
import jcraft.jblockactivity.listeners.BlockBreakListener;
import jcraft.jblockactivity.listeners.BlockInteractListener;
import jcraft.jblockactivity.listeners.BlockPlaceListener;
import jcraft.jblockactivity.listeners.InventoryAccessListener;
import jcraft.jblockactivity.listeners.LogToolListener;
import jcraft.jblockactivity.sql.SQLConnection;
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

    public void onEnable() {
        dataFolder = getDataFolder();
        blockActivity = this;

        config = new ActivityConfig();
        config.loadConfig();

        try {
            connectSQL();
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Polaczenie z baza nie powiodlo sie!");
            e.printStackTrace();
            this.getPluginLoader().disablePlugin(this);
            return;
        }
        try {
            createBasicTables();
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Nie mozna utworzyc podstawowych tabel!");
            e.printStackTrace();
            this.getPluginLoader().disablePlugin(this);
            return;
        }

        config.loadWorldConfig();

        cmdHandler = new CommandHandler();
        this.getCommand("ba").setExecutor(cmdHandler);

        logExecuteRunnable = new LogExecuteThread(config.maxTimePerRun, config.timeBetweenRuns, config.minLogsToProcess);
        logExecuteThread = new Thread(logExecuteRunnable, "jBlockLogExecutor");
        logExecuteThread.start();

        actionExecuteRunnable = new ActionExecuteThread(cmdHandler);
        actionExecuteThread = new Thread(actionExecuteRunnable, "jBlockActionExecutor");
        actionExecuteThread.start();

        final PluginManager manager = this.getServer().getPluginManager();
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
        manager.registerEvents(new LogToolListener(), this);
    }

    public void onDisable() {
        try {
            logExecuteRunnable.terminate();
            logExecuteThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void connectSQL() throws SQLException {
        final SQLConnection sqlConnection = new SQLConnection(sqlProfile);
        sqlConnection.open();
        sqlConnection.closeConnection();
    }

    public void createBasicTables() throws SQLException {
        final SQLConnection sqlConnection = new SQLConnection(sqlProfile);
        sqlConnection.open();
        final Statement state = sqlConnection.getConnection().createStatement();
        state.executeUpdate("CREATE TABLE IF NOT EXISTS `ba-players` (playerid INT UNSIGNED NOT NULL AUTO_INCREMENT, playername varchar(32) NOT NULL, primary key (playerid))");
        state.close();
        sqlConnection.closeConnection();
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
        return config.hiddenPlayers.contains(playerName.toLowerCase());
    }

    public static boolean hidePlayer(String playerName) {
        playerName = playerName.toLowerCase();
        if (config.hiddenPlayers.contains(playerName)) {
            config.hiddenPlayers.remove(playerName);
            return false;
        }
        config.hiddenPlayers.add(playerName);
        return true;
    }

}
