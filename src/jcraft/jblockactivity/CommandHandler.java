package jcraft.jblockactivity;

import static jcraft.jblockactivity.utils.ActivityUtil.isInt;
import static jcraft.jblockactivity.utils.ActivityUtil.saveSpawnHeight;
import static org.bukkit.Bukkit.getLogger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import jcraft.jblockactivity.ActionExecuteThread.ActionRequest;
import jcraft.jblockactivity.ActionExecuteThread.ActionRequest.ActionType;
import jcraft.jblockactivity.editor.BlockEditor;
import jcraft.jblockactivity.editor.BlockEditorResult;
import jcraft.jblockactivity.session.ActiveSession;
import jcraft.jblockactivity.session.LookupCache;
import jcraft.jblockactivity.session.LookupCacheFactory;
import jcraft.jblockactivity.utils.ImportQueryGen;
import jcraft.jblockactivity.utils.ImportQueryGen.ImportPlugin;
import jcraft.jblockactivity.utils.QueryParams;
import jcraft.jblockactivity.utils.QueryParams.Order;
import jcraft.jblockactivity.utils.QueryParams.SummarizationMode;
import jcraft.jblockactivity.utils.question.ClearlogQuestion;
import jcraft.jblockactivity.utils.question.QuestionData;
import jcraft.jblockactivity.utils.question.RedoQuestion;
import jcraft.jblockactivity.utils.question.RollbackQuestion;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        try {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.GOLD + "jBlockActivity" + ChatColor.GRAY + " version "
                        + BlockActivity.getBlockActivity().getDescription().getVersion() + " by " + ChatColor.GOLD + "KodekPL");
                sender.sendMessage(ChatColor.GRAY + "Visit " + ChatColor.GOLD + BlockActivity.getBlockActivity().getDescription().getWebsite() + " "
                        + ChatColor.GRAY + "for more information!");
                return true;
            } else {
                if (args[0].equalsIgnoreCase("tool") || args[0].equalsIgnoreCase("blocktool")) {
                    if (!sender.hasPermission("ba.tool.lookup")) {
                        sender.sendMessage(BlockActivity.prefix + ChatColor.RED + "You don't have required permission - ba.tool.lookup");
                        return false;
                    }
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(BlockActivity.prefix + ChatColor.RED + "You have to be a player.");
                        return false;
                    }
                    final Player player = (Player) sender;
                    final boolean block = args[0].equalsIgnoreCase("blocktool");
                    if (block && sender.hasPermission("ba.toolblock.spawn")) {
                        BlockActivity.logBlockTool.giveTool(player);
                        return true;
                    } else if (sender.hasPermission("ba.toolitem.spawn")) {
                        BlockActivity.logItemTool.giveTool(player);
                        return true;
                    }
                    sender.sendMessage(BlockActivity.prefix + ChatColor.RED
                            + "You don't have required permission - ba.toolblock.spawn / ba.toolitem.spawn");
                    return false;
                } else if (args[0].equalsIgnoreCase("hide")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(BlockActivity.prefix + ChatColor.RED + "You have to be a player.");
                        return false;
                    }
                    if (!sender.hasPermission("ba.hide")) {
                        sender.sendMessage(BlockActivity.prefix + ChatColor.RED + "You don't have required permission - ba.hide");
                        return false;
                    }

                    if (BlockActivity.hidePlayer(sender.getName())) {
                        sender.sendMessage(BlockActivity.prefix + ChatColor.GREEN + "You are now hidden and aren't logged.");
                    } else {
                        sender.sendMessage(BlockActivity.prefix + ChatColor.GREEN + "You aren't hidden anylonger.");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("savequeue") && sender.hasPermission("ba.savequeue")) {
                    final LogExecuteThread executor = BlockActivity.getLogExecuteThread();
                    sender.sendMessage(ChatColor.GOLD + "Current queue size: " + executor.getQueueSize());
                    int lastSize = -1, fails = 0;
                    while (executor.getQueueSize() > 0) {
                        fails = (lastSize == executor.getQueueSize()) ? fails + 1 : 0;
                        if (fails > 10) {
                            sender.sendMessage(BlockActivity.prefix + ChatColor.RED + "Unable to save queue!");
                            return false;
                        }
                        lastSize = executor.getQueueSize();
                        executor.notifyLock();
                    }
                    sender.sendMessage(BlockActivity.prefix + ChatColor.GREEN + "Queue saved successfully.");
                    return true;
                } else if (args[0].equals("rollback") || args[0].equals("undo") || args[0].equals("rb")) {
                    if (!sender.hasPermission("ba.rollback")) {
                        sender.sendMessage(BlockActivity.prefix + ChatColor.RED + "You don't have required permission - ba.rollback");
                        return false;
                    }
                    preExecuteCommand(new ActionRequest(ActionType.CMD_ROLLBACK, sender, args, true), true);
                    return true;
                } else if (args[0].equals("redo")) {
                    if (!sender.hasPermission("ba.rollback")) {
                        sender.sendMessage(BlockActivity.prefix + ChatColor.RED + "You don't have required permission - ba.rollback");
                        return false;
                    }
                    preExecuteCommand(new ActionRequest(ActionType.CMD_REDO, sender, args, true), true);
                    return true;
                } else if (args[0].equalsIgnoreCase("page")) {
                    if (args.length == 2 && isInt(args[1])) {
                        showPage(sender, Integer.valueOf(args[1]));
                    } else {
                        sender.sendMessage(BlockActivity.prefix + ChatColor.RED + "You have to specify a page numer!");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("tp")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(BlockActivity.prefix + ChatColor.RED + "You have to be a player.");
                        return false;
                    }
                    if (!sender.hasPermission("ba.teleport")) {
                        sender.sendMessage(BlockActivity.prefix + ChatColor.RED + "You don't have required permission - ba.teleport");
                        return false;
                    }
                    if (args.length != 2 || !isInt(args[1])) {
                        sender.sendMessage(BlockActivity.prefix + ChatColor.RED + "You have to specify a log ID numer!");
                        return false;
                    }

                    final int pos = Integer.parseInt(args[1]) - 1;
                    final Player player = (Player) sender;
                    final ActiveSession session = ActiveSession.getSession(player);

                    if (session.lookupCache == null) {
                        sender.sendMessage(BlockActivity.prefix + ChatColor.RED + "You havn't done a lookup yet!");
                        return false;
                    }

                    if (pos >= 0 && pos < session.lookupCache.length) {
                        final Location loc = session.lookupCache[pos].getLocation();
                        if (loc != null) {
                            player.teleport(new Location(loc.getWorld(), loc.getX() + 0.5, saveSpawnHeight(loc), loc.getZ() + 0.5, player
                                    .getLocation().getYaw(), player.getLocation().getPitch()));
                            player.sendMessage(BlockActivity.prefix + ChatColor.GOLD + "Teleported to " + loc.getBlockX() + ":" + loc.getBlockY()
                                    + ":" + loc.getBlockZ());
                            return true;
                        } else {
                            sender.sendMessage(BlockActivity.prefix + ChatColor.RED + "There is no location! Add 'coords' parameter to lookup.");
                            return false;
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "'" + args[1] + " is out of range");
                        return false;
                    }
                } else if (args[0].equalsIgnoreCase("yes") || args[0].equalsIgnoreCase("no")) {
                    if (!sender.hasPermission("ba.answer")) {
                        sender.sendMessage(BlockActivity.prefix + ChatColor.RED + "You don't have required permission - ba.answer");
                        return false;
                    }
                    final ActiveSession session = ActiveSession.getSession(sender);
                    if (session.question == null) {
                        sender.sendMessage(BlockActivity.prefix + ChatColor.RED + "You are not asked for anything!");
                        return false;
                    }
                    final boolean answer = args[0].equalsIgnoreCase("yes");
                    if (answer) {
                        preExecuteCommand(new ActionRequest(ActionType.CMD_CONFIRM, sender, new String[0], true), true);
                    } else {
                        answerQuestion(sender, answer);
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("lookup") && sender.hasPermission("ba.lookup")) {
                    preExecuteCommand(new ActionRequest(ActionType.CMD_LOOKUP, sender, args, false), true);
                    return true;
                } else if (args[0].equalsIgnoreCase("clearlog") && sender.hasPermission("ba.clearlog")) {
                    preExecuteCommand(new ActionRequest(ActionType.CMD_CLEARLOG, sender, args, true), true);
                    return true;
                } else if (args[0].equalsIgnoreCase("importlogs") && sender.hasPermission("ba.admin")) {
                    if (args.length == 4) {
                        ImportPlugin pluginName;
                        try {
                            pluginName = ImportQueryGen.ImportPlugin.valueOf(args[1].toUpperCase());
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage(BlockActivity.prefix + ChatColor.RED + "There is no option of importing for plugin '"
                                    + args[1].toUpperCase() + "'");
                            return false;
                        }
                        if (ImportQueryGen.createImportFile(pluginName, args[2], args[3])) {
                            sender.sendMessage(BlockActivity.prefix + ChatColor.GREEN + "File with SQL query was saved in plugin directory!");
                            return true;
                        } else {
                            sender.sendMessage(BlockActivity.prefix + ChatColor.RED + "There is no logged world called '" + args[3] + "'");
                            return false;
                        }
                    } else {
                        sender.sendMessage(BlockActivity.prefix + ChatColor.RED + "You have to specify plugin, old table name and world name!");
                        sender.sendMessage(BlockActivity.prefix + ChatColor.YELLOW + "Command example: /ba importlogs LOGBLOCK lb-world world");
                        return false;
                    }
                }
            }
        } catch (final IllegalArgumentException ex) {
            sender.sendMessage(ChatColor.RED + ex.getMessage());
        }
        sender.sendMessage(BlockActivity.prefix + ChatColor.RED + "Unknown command '" + args[0] + "'.");
        return false;
    }

    public void preExecuteCommand(ActionRequest request, boolean async) throws IllegalArgumentException {
        if (async) {
            ActionExecuteThread.addRequest(request);
        } else {
            executeCommand(request);
        }
    }

    public void executeCommand(ActionRequest request) throws IllegalArgumentException {
        switch (request.getType()) {
        case CMD_LOOKUP:
            lookupCmd(request.getSender(), request.getParams());
            break;
        case CMD_CLEARLOG:
            clearlogCmd(request.getSender(), request.getParams(), request.askQuestion());
            break;
        case CMD_ROLLBACK:
            rollbackCmd(request.getSender(), request.getParams(), request.askQuestion());
            break;
        case CMD_REDO:
            redoCmd(request.getSender(), request.getParams(), request.askQuestion());
            break;
        case CMD_CONFIRM:
            answerQuestion(request.getSender(), true);
            break;
        default:
            break;
        }
    }

    private void showPage(CommandSender sender, int page) {
        final ActiveSession session = ActiveSession.getSession(sender);
        if (session.lookupCache != null && session.lookupCache.length > 0) {
            if (session.lookupCache.length >= BlockActivity.config.linesPerPage) {
                sender.sendMessage(ChatColor.GOLD.toString() + session.lookupCache.length + " changes found.");
            }
            final int startpos = (page - 1) * BlockActivity.config.linesPerPage;
            if (page > 0 && startpos <= session.lookupCache.length - 1) {
                final int stoppos = (startpos + BlockActivity.config.linesPerPage >= session.lookupCache.length) ? session.lookupCache.length - 1
                        : startpos + BlockActivity.config.linesPerPage - 1;
                final int numberOfPages = (int) Math.ceil(session.lookupCache.length / (double) BlockActivity.config.linesPerPage);
                if (numberOfPages != 1) {
                    sender.sendMessage(ChatColor.GRAY + "Page " + page + "/" + numberOfPages);
                }
                for (int i = startpos; i <= stoppos; i++) {
                    if (session.lookupCache[i] == null) {
                        continue;
                    }
                    sender.sendMessage(((session.lookupCache[i].getLocation() != null) ? "[" + (i + 1) + "] " : "")
                            + session.lookupCache[i].getMessage());
                }
                sender.sendMessage(ChatColor.GOLD + "-------------------");
            } else {
                sender.sendMessage(ChatColor.RED + "There isn't a page #" + page + "!");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "No blocks in lookup cache!");
        }
    }

    public void lookupCmd(CommandSender sender, QueryParams params) {
        Connection connection = null;
        Statement state = null;
        ResultSet result = null;
        try {
            connection = BlockActivity.getBlockActivity().getConnection();
            if (connection == null) {
                sender.sendMessage(ChatColor.RED + "MySQL connection lost!");
                return;
            }
            state = connection.createStatement();
            result = state.executeQuery(params.getQuery());

            if (result.next()) {
                result.beforeFirst();
                final List<LookupCache> lookupLogs = new ArrayList<LookupCache>();
                final LookupCacheFactory logsFactory = new LookupCacheFactory(params, sender instanceof Player ? 2F / 3F : 1F);
                while (result.next()) {
                    lookupLogs.add(logsFactory.getLookupCache(result));
                }
                final ActiveSession session = ActiveSession.getSession(sender);
                session.lookupCache = lookupLogs.toArray(new LookupCache[lookupLogs.size()]);
                session.lastParams = params;

                if (params.mode != SummarizationMode.NONE) {
                    if (params.mode == SummarizationMode.BLOCKS) {
                        sender.sendMessage(ChatColor.GOLD + "Created - Destroyed - Block");
                    } else if (params.mode == SummarizationMode.ENTITIES) {
                        sender.sendMessage(ChatColor.GOLD + "Kills - Entity");
                    } else {
                        sender.sendMessage(ChatColor.GOLD + "Created - Destroyed - Player");
                    }
                }

                showPage(sender, 1);
            } else {
                sender.sendMessage(ChatColor.GOLD + "No results found.");
            }
        } catch (SQLException e1) {
            sender.sendMessage(ChatColor.RED + "Exception, check error log!");
            getLogger().log(Level.SEVERE, "[Lookup] " + params.getQuery() + ": ", e1);
            e1.printStackTrace();
        } catch (IllegalArgumentException e2) {
            sender.sendMessage(ChatColor.RED + e2.getMessage());
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
                getLogger().log(Level.SEVERE, "[CommandsHandler] SQL Exception on close!", e);
            }
        }
    }

    public void clearlogCmd(CommandSender sender, QueryParams params, boolean askQuestion) {
        Connection connection = null;
        Statement state = null;
        ResultSet result = null;
        try {
            connection = BlockActivity.getBlockActivity().getConnection();
            if (connection == null) {
                sender.sendMessage(ChatColor.RED + "MySQL connection lost!");
                return;
            }
            state = connection.createStatement();

            int deleted = 0;
            final String join = (params.players.size() > 0) ? "INNER JOIN `ba-players` USING (playerid) " : "";

            result = state.executeQuery("SELECT count(*) FROM " + params.getTable() + join + params.getWhere(LoggingType.all));
            result.next();
            deleted = result.getInt(1);
            if (deleted > 0) {
                sender.sendMessage(ChatColor.GOLD.toString() + deleted + " changes found.");
                final ActiveSession session = ActiveSession.getSession(sender);

                final ClearlogQuestion question = new ClearlogQuestion();
                question.setParams(params);
                session.question = question;

                if (askQuestion && BlockActivity.config.askClearlogs) {
                    askQuestion(sender);
                } else {
                    answerQuestion(sender, true);
                }
            } else {
                sender.sendMessage(ChatColor.GOLD + "No changes found.");
            }
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Exception, check error log!");
            getLogger().log(Level.SEVERE, "[ClearLog] " + params.getQuery() + ": ", e);
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
                getLogger().log(Level.SEVERE, "[CommandsHandler] SQL Exception on close!", e);
            }
        }
    }

    public void rollbackCmd(CommandSender sender, QueryParams params, boolean askQuestion) {
        Connection connection = null;
        Statement state = null;
        ResultSet result = null;

        params.logType = LoggingType.all;
        params.needCoords = true;
        params.needMaterial = true;
        params.needData = true;
        params.needPlayer = true;
        params.needTime = true;
        params.needLogType = true;
        params.needExtraData = true;
        params.order = Order.DESC;
        params.mode = SummarizationMode.NONE;

        try {
            connection = BlockActivity.getBlockActivity().getConnection();
            if (connection == null) {
                sender.sendMessage(ChatColor.RED + "MySQL connection lost!");
                return;
            }
            state = connection.createStatement();
            result = state.executeQuery(params.getQuery());

            final BlockEditor editor = new BlockEditor(params.getWorld(), false);

            final LookupCacheFactory logsFactory = new LookupCacheFactory(params, 0F);
            while (result.next()) {
                editor.addChange(logsFactory.getLookupCache(result));
            }

            final int changes = editor.getSize();
            if (changes > 10000) {
                editor.setSender(sender);
            }
            sender.sendMessage(ChatColor.GOLD.toString() + changes + " changes found.");
            if (changes == 0) {
                sender.sendMessage(ChatColor.RED + "Rollback aborted!");
                return;
            }
            final ActiveSession session = ActiveSession.getSession(sender);

            final RollbackQuestion question = new RollbackQuestion();
            question.setEditor(editor);
            session.question = question;
            if (askQuestion && BlockActivity.config.askRollbacks) {
                askQuestion(sender);
            } else {
                answerQuestion(sender, true);
            }
        } catch (SQLException | Error e) {
            sender.sendMessage(ChatColor.RED + "Exception, check error log!");
            getLogger().log(Level.SEVERE, "[Rollback] " + params.getQuery() + ": ", e);
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
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
                getLogger().log(Level.SEVERE, "[CommandsHandler] SQL Exception on close!", e);
            }
        }
    }

    public void redoCmd(CommandSender sender, QueryParams params, boolean askQuestion) {
        Connection connection = null;
        Statement state = null;
        ResultSet result = null;

        params.logType = LoggingType.all;
        params.needCoords = true;
        params.needMaterial = true;
        params.needData = true;
        params.needPlayer = true;
        params.needTime = true;
        params.needLogType = true;
        params.needExtraData = true;
        params.order = Order.ASC;
        params.mode = SummarizationMode.NONE;

        try {
            connection = BlockActivity.getBlockActivity().getConnection();
            if (connection == null) {
                sender.sendMessage(ChatColor.RED + "MySQL connection lost!");
                return;
            }
            state = connection.createStatement();
            result = state.executeQuery(params.getQuery());

            final BlockEditor editor = new BlockEditor(params.getWorld(), true);

            final LookupCacheFactory logsFactory = new LookupCacheFactory(params, 0F);
            while (result.next()) {
                editor.addChange(logsFactory.getLookupCache(result));
            }

            final int changes = editor.getSize();
            if (changes > 10000) {
                editor.setSender(sender);
            }
            sender.sendMessage(ChatColor.GOLD.toString() + changes + " changes found.");
            if (changes == 0) {
                sender.sendMessage(ChatColor.RED + "Redo aborted!");
                return;
            }
            final ActiveSession session = ActiveSession.getSession(sender);

            final RedoQuestion question = new RedoQuestion();
            question.setEditor(editor);
            session.question = question;

            if (askQuestion && BlockActivity.config.askRedos) {
                askQuestion(sender);
            } else {
                answerQuestion(sender, true);
            }
        } catch (SQLException | Error e) {
            sender.sendMessage(ChatColor.RED + "Exception, check error log!");
            getLogger().log(Level.SEVERE, "[Redo] " + params.getQuery() + ": ", e);
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
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
                getLogger().log(Level.SEVERE, "[CommandsHandler] SQL Exception on close!", e);
            }
        }
    }

    private void askQuestion(CommandSender sender) {
        sender.sendMessage(BlockActivity.prefix + ChatColor.YELLOW + "Are you sure you want to continue?");
        sender.sendMessage(ChatColor.GREEN + "/ba yes" + "\n" + "/ba no");
    }

    private void answerQuestion(CommandSender sender, boolean answer) {
        final ActiveSession session = ActiveSession.getSession(sender);
        final QuestionData question = session.question;
        if (!answer) {
            if (question instanceof RollbackQuestion) {
                sender.sendMessage(ChatColor.RED + "Rollback aborted!");
            } else if (question instanceof RedoQuestion) {
                sender.sendMessage(ChatColor.RED + "Redo aborted!");
            } else if (question instanceof ClearlogQuestion) {
                sender.sendMessage(ChatColor.RED + "Clearlog aborted!");
            }
        } else {
            try {
                if (question instanceof RollbackQuestion) {
                    final BlockEditor editor = ((RollbackQuestion) session.question).getEditor();
                    editor.start();
                    session.lookupCache = editor.errors;
                    sender.sendMessage(ChatColor.GREEN + "Rollback finished successfully (" + editor.getElapsedTime() + " ms"
                            + ((editor.getErrors() > 0) ? ", " + ChatColor.RED + editor.getErrors() + " errors" + ChatColor.GREEN : "") + ")");

                    final int blockChanges = editor.getResults(BlockEditorResult.BLOCK_CREATED) + editor.getResults(BlockEditorResult.BLOCK_REMOVED)
                            + editor.getResults(BlockEditorResult.BLOCK_BLACKLIST) + editor.getResults(BlockEditorResult.NO_BLOCK_ACTION);
                    final int inventoryChanges = editor.getResults(BlockEditorResult.INVENTORY_ACCESS)
                            + editor.getResults(BlockEditorResult.NO_INVENTORY_ACTION);
                    final int hangingChanges = editor.getResults(BlockEditorResult.HANGING_SPAWNED)
                            + editor.getResults(BlockEditorResult.HANGING_REMOVED) + editor.getResults(BlockEditorResult.NO_HANGING_ACTION);
                    final int entityChanges = editor.getResults(BlockEditorResult.ENTITY_SPAWNED)
                            + editor.getResults(BlockEditorResult.NO_ENTITY_ACTION);

                    if (blockChanges > 0) {
                        sender.sendMessage(ChatColor.GREEN + " Changed blocks: "
                                + (editor.getResults(BlockEditorResult.BLOCK_CREATED) + editor.getResults(BlockEditorResult.BLOCK_REMOVED)) + "/"
                                + blockChanges);
                    }
                    if (hangingChanges > 0) {
                        sender.sendMessage(ChatColor.GREEN + " Changed hangings: "
                                + (editor.getResults(BlockEditorResult.HANGING_SPAWNED) + editor.getResults(BlockEditorResult.HANGING_REMOVED)) + "/"
                                + hangingChanges);
                    }
                    if (inventoryChanges > 0) {
                        sender.sendMessage(ChatColor.GREEN + " Changed inventories: " + editor.getResults(BlockEditorResult.INVENTORY_ACCESS) + "/"
                                + inventoryChanges);
                    }
                    if (entityChanges > 0) {
                        sender.sendMessage(ChatColor.GREEN + " Spawned entities: " + editor.getResults(BlockEditorResult.ENTITY_SPAWNED) + "/"
                                + entityChanges);
                    }
                } else if (question instanceof RedoQuestion) {
                    final BlockEditor editor = ((RedoQuestion) session.question).getEditor();
                    editor.start();
                    session.lookupCache = editor.errors;
                    sender.sendMessage(ChatColor.GREEN + "Redo finished successfully (" + editor.getElapsedTime() + " ms"
                            + ((editor.getErrors() > 0) ? ", " + ChatColor.RED + editor.getErrors() + " errors" + ChatColor.GREEN : "") + ")");

                    final int blockChanges = editor.getResults(BlockEditorResult.BLOCK_CREATED) + editor.getResults(BlockEditorResult.BLOCK_REMOVED)
                            + editor.getResults(BlockEditorResult.BLOCK_BLACKLIST) + editor.getResults(BlockEditorResult.NO_BLOCK_ACTION);
                    final int inventoryChanges = editor.getResults(BlockEditorResult.INVENTORY_ACCESS)
                            + editor.getResults(BlockEditorResult.NO_INVENTORY_ACTION);
                    final int hangingChanges = editor.getResults(BlockEditorResult.HANGING_SPAWNED)
                            + editor.getResults(BlockEditorResult.HANGING_REMOVED) + editor.getResults(BlockEditorResult.NO_HANGING_ACTION);
                    final int entityChanges = editor.getResults(BlockEditorResult.ENTITY_SPAWNED)
                            + editor.getResults(BlockEditorResult.NO_ENTITY_ACTION);

                    if (blockChanges > 0) {
                        sender.sendMessage(ChatColor.GREEN + " Changed blocks: "
                                + (editor.getResults(BlockEditorResult.BLOCK_CREATED) + editor.getResults(BlockEditorResult.BLOCK_REMOVED)) + "/"
                                + blockChanges);
                    }
                    if (hangingChanges > 0) {
                        sender.sendMessage(ChatColor.GREEN + " Changed hangings: "
                                + (editor.getResults(BlockEditorResult.HANGING_SPAWNED) + editor.getResults(BlockEditorResult.HANGING_REMOVED)) + "/"
                                + hangingChanges);
                    }
                    if (inventoryChanges > 0) {
                        sender.sendMessage(ChatColor.GREEN + " Changed inventories: " + editor.getResults(BlockEditorResult.INVENTORY_ACCESS) + "/"
                                + inventoryChanges);
                    }
                    if (entityChanges > 0) {
                        sender.sendMessage(ChatColor.GREEN + " Spawned entities: " + editor.getResults(BlockEditorResult.ENTITY_SPAWNED) + "/"
                                + entityChanges);
                    }
                } else if (question instanceof ClearlogQuestion) {
                    final QueryParams params = ((ClearlogQuestion) session.question).getParams();
                    Connection connection = null;
                    Statement state = null;
                    ResultSet result = null;
                    try {
                        connection = BlockActivity.getBlockActivity().getConnection();
                        if (connection == null) {
                            sender.sendMessage(ChatColor.RED + "MySQL connection lost!");
                            return;
                        }
                        state = connection.createStatement();

                        int deleted = 0;
                        final String join = (params.players.size() > 0) ? "INNER JOIN `ba-players` USING (playerid) " : "";

                        result = state.executeQuery("SELECT count(*) FROM " + params.getTable() + join + params.getWhere(LoggingType.all));
                        result.next();
                        deleted = result.getInt(1);
                        if (deleted > 0) {
                            state.execute("DELETE " + params.getTable() + " FROM " + params.getTable() + join + params.getWhere(LoggingType.all));
                            sender.sendMessage(ChatColor.GREEN + "Cleared out table " + params.getTable() + ". Deleted " + deleted + " entries.");
                        }
                        result.close();

                        result = state.executeQuery("SELECT COUNT(*) FROM " + params.getTable("-extra") + " LEFT JOIN " + params.getTable()
                                + " USING (id) WHERE " + params.getTable() + ".id IS NULL");
                        result.next();
                        deleted = result.getInt(1);
                        if (deleted > 0) {
                            state.execute("DELETE " + params.getTable("-extra") + " FROM " + params.getTable("-extra") + " LEFT JOIN "
                                    + params.getTable() + " USING (id) WHERE " + params.getTable() + ".id IS NULL;");
                            sender.sendMessage(ChatColor.GREEN + "Cleared out table " + params.getTable("-extra") + ". Deleted " + deleted
                                    + " entries.");
                        }
                    } catch (Exception e) {
                        sender.sendMessage(ChatColor.RED + "Exception, check error log!");
                        getLogger().log(Level.SEVERE, "[ClearLog] " + params.getQuery() + ": ", e);
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
                            getLogger().log(Level.SEVERE, "[CommandsHandler] SQL Exception on close!", e);
                        }
                    }
                }
            } catch (InterruptedException | Error e) {
                sender.sendMessage(ChatColor.RED + "Exception, check error log!");
                e.printStackTrace();
            }
        }
        session.question = null;
    }
}
