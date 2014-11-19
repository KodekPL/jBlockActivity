package jcraft.jblockactivity;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Pattern;

import jcraft.jblockactivity.actionlog.ActionLog;
import jcraft.jblockactivity.config.ActivityConfig;
import jcraft.jblockactivity.config.WorldConfig;
import jcraft.jblockactivity.event.ActionLogQueueEvent;
import jcraft.jblockactivity.listeners.BlockBreakListener;
import jcraft.jblockactivity.listeners.BlockBurnListener;
import jcraft.jblockactivity.listeners.BlockInteractListener;
import jcraft.jblockactivity.listeners.BlockPlaceListener;
import jcraft.jblockactivity.listeners.CreatureKillListener;
import jcraft.jblockactivity.listeners.ExplosionListener;
import jcraft.jblockactivity.listeners.HangingListener;
import jcraft.jblockactivity.listeners.InventoryAccessListener;
import jcraft.jblockactivity.listeners.LogToolListener;
import jcraft.jblockactivity.listeners.WorldEditListener;
import jcraft.jblockactivity.session.LookupCacheFactory;
import jcraft.jblockactivity.sql.SQLConnectionPool;
import jcraft.jblockactivity.sql.SQLProfile;
import jcraft.jblockactivity.tool.LogTool;
import jcraft.jblockactivity.utils.MaterialNames;
import jcraft.jblockactivity.utils.QueryParams;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;

public class BlockActivity extends JavaPlugin {

    public final static int LATEST_VERSION = 3;
    public final static int MIN_WE_BUILD = 3282;

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
            createGlobalTables();
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
        config.checkConfigVersion();

        cmdHandler = new CommandHandler();
        getCommand("ba").setExecutor(cmdHandler);

        logExecuteRunnable = new LogExecuteThread(config.maxTimePerRun, config.timeBetweenRuns, config.minLogsToProcess, config.queueWarningSize);
        logExecuteThread = new Thread(logExecuteRunnable, "jBlockLogExecutor");
        logExecuteThread.start();

        actionExecuteRunnable = new ActionExecuteThread(cmdHandler);
        actionExecuteThread = new Thread(actionExecuteRunnable, "jBlockActionExecutor");
        actionExecuteThread.start();

        MaterialNames.loadMaterialNames();

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
        if (config.isWorldsLogging(LoggingType.blockinteract) || config.isWorldsLogging(LoggingType.tramplefarmland)) {
            manager.registerEvents(new BlockInteractListener(), this);
        }
        if (config.isWorldsLogging(LoggingType.hangingbreak) || config.isWorldsLogging(LoggingType.hangingplace)
                || config.isWorldsLogging(LoggingType.hanginginteract)) {
            manager.registerEvents(new HangingListener(), this);
        }
        if (config.isWorldsLogging(LoggingType.creaturekill)) {
            manager.registerEvents(new CreatureKillListener(), this);
        }
        if (config.isWorldsLogging(LoggingType.explosions)) {
            manager.registerEvents(new ExplosionListener(), this);
        }
        if (config.isWorldsLogging(LoggingType.blockburn)) {
            manager.registerEvents(new BlockBurnListener(), this);
        }
        if (config.isWorldsLogging(LoggingType.worldedit)) {
            hookWorldEditPlugin();
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

    public void hookWorldEditPlugin() {
        final Plugin worldEdit = getServer().getPluginManager().getPlugin("WorldEdit");
        if (worldEdit != null) {
            final String bukkitVersion = worldEdit.getDescription().getVersion();
            final Pattern pattern = Pattern.compile("[\\*.-]");

            if (!config.forceWorldEditPlugin) {
                int build = -1;
                try {
                    build = Integer.parseInt(pattern.split(bukkitVersion)[4]);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    getLogger()
                            .log(Level.SEVERE,
                                    "Plugin was not able to read version of WorldEdit installed on this server. "
                                            + "Make sure your WorldEdit is up to date. "
                                            + "If you think your version is correct, turn on 'forceWorldEditPlugin' in jBlockActivity configuration file to force WorldEdit support. "
                                            + "This option may cause issues if your WorldEdit is older than required.");
                    return;
                }

                if (build != -1 && build < MIN_WE_BUILD) {
                    getLogger()
                            .log(Level.SEVERE,
                                    "Version of WorldEdit installed on this server is lower than required by jBlockActivity! "
                                            + "Install WorldEdit build "
                                            + MIN_WE_BUILD
                                            + " or higher, to run support for WorldEdit. "
                                            + "If you think your version is correct, turn on 'forceWorldEditPlugin' in jBlockActivity configuration file to force WorldEdit support. "
                                            + "This option may cause issues if your WorldEdit is older than required.");
                    return;
                }
            }

            final WorldEditPlugin worldEditPlugin = (WorldEditPlugin) worldEdit;
            worldEditPlugin.getWorldEdit().getEventBus().register(new WorldEditListener());
            getLogger().log(Level.INFO, "Plugin WorldEdit was hooked correctly to jBlockActivity.");
        } else {
            getLogger().log(Level.SEVERE,
                    "Server was unable to find WorldEdit plugin installed. Make sure WorldEdit is located in your plugins folder.");
        }
    }

    private void createGlobalTables() throws SQLException {
        final Connection connection = getConnection();
        if (connection == null) {
            throw new SQLException("No connection!");
        }
        final Statement state = connection.createStatement();
        state.executeUpdate("CREATE TABLE IF NOT EXISTS `ba-players` (playerid MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT, playername varchar(32) NOT NULL, uuid varchar(32), primary key (playerid), UNIQUE (playername), UNIQUE (uuid));");
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
            if (config.callActionLogQueueEvent) {
                ActionLogQueueEvent event = new ActionLogQueueEvent(action);
                Bukkit.getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    logExecuteRunnable.queue.add(event.getActionLog());
                }
            } else {
                logExecuteRunnable.queue.add(action);
            }
        }
    }

    public static LogExecuteThread getLogExecuteThread() {
        return logExecuteRunnable;
    }

    public static List<ActionLog> getActionLogs(QueryParams params) {
        final List<ActionLog> lookupLogs = new ArrayList<ActionLog>();
        Connection connection = null;
        Statement state = null;
        ResultSet result = null;
        try {
            connection = BlockActivity.getBlockActivity().getConnection();
            if (connection == null) {
                return lookupLogs;
            }
            state = connection.createStatement();
            result = state.executeQuery(params.getQuery());

            if (result.next()) {
                result.beforeFirst();

                final LookupCacheFactory logsFactory = new LookupCacheFactory(params, 0);
                while (result.next()) {
                    lookupLogs.add(logsFactory.getLookupCache(result).getActionLog());
                }
                return lookupLogs;
            } else {
                return lookupLogs;
            }
        } catch (SQLException e1) {
            org.bukkit.Bukkit.getLogger().log(Level.SEVERE, "[jBA-Lookup] " + params.getQuery() + ": ", e1);
            e1.printStackTrace();
        } catch (IllegalArgumentException e2) {
            return lookupLogs;
        } finally {
            try {
                if (result != null) {
                    result.close();
                }
                if (state != null) {
                    state.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
            }
        }
        return lookupLogs;
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
            final MaterialData material = logItemTool.getItemMaterial();
            if (material.getItemTypeId() == data.getItemTypeId() && material.getData() == data.getData()) {
                return logItemTool;
            }
        }
        if (logBlockTool != null) {
            final MaterialData material = logBlockTool.getItemMaterial();
            if (material.getItemTypeId() == data.getItemTypeId() && material.getData() == data.getData()) {
                return logBlockTool;
            }
        }
        return null;
    }

    public static boolean isHidden(UUID uuid) {
        return isHidden(uuid.toString());
    }

    public static boolean isHidden(String name) {
        return config.hiddenPlayers.contains(name);
    }

    public static boolean hidePlayer(String name) {
        if (config.hiddenPlayers.contains(name)) {
            config.hiddenPlayers.remove(name);
            return false;
        }
        config.hiddenPlayers.add(name);
        return true;
    }

    public static String getPlayerId(String identifier) {
        if (identifier == null) {
            return "NULL";
        }
        final Integer id = BlockActivity.playerIds.get(identifier);
        if (id != null) {
            return id.toString();
        }
        return "(SELECT playerid FROM `ba-players` WHERE uuid = '" + identifier + "')";
    }

}
