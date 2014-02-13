package jcraft.jblockactivity.utils;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.actionlog.BlockActionLog;
import jcraft.jblockactivity.extradata.BlockExtraData;
import jcraft.jblockactivity.extradata.ExtraData;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.QueryParams.BlockChangeType;

public class LogBlockConverter extends BukkitRunnable {

    public static boolean RUNNING = false;

    private final CommandSender sender;
    private final World world;

    private final Queue<de.diddiz.LogBlock.BlockChange> changes = new LinkedList<de.diddiz.LogBlock.BlockChange>();
    private LogBlock logBlock;
    private int fullQueue = 0, progress = 0;
    private BukkitTask task;

    public LogBlockConverter(CommandSender sender, World world) {
        this.sender = sender;
        this.world = world;
    }

    public boolean setupLogBlock() {
        final BlockActivity blockActivity = BlockActivity.getBlockActivity();
        final Plugin plugin = blockActivity.getServer().getPluginManager().getPlugin("LogBlock");
        if (plugin != null) {
            logBlock = (LogBlock) plugin;
            return true;
        }
        return false;
    }

    public boolean setupBlockChanges() {
        try {
            final de.diddiz.LogBlock.QueryParams params = new de.diddiz.LogBlock.QueryParams(logBlock);
            params.bct = BlockChangeType.ALL;
            params.limit = -1;
            params.world = world;
            params.needDate = true;
            params.needType = true;
            params.needData = true;
            params.needPlayer = true;
            params.needSignText = true;
            params.needCoords = true;

            changes.addAll(logBlock.getBlockChanges(params));
            fullQueue = changes.size();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean start() {
        if (changes.isEmpty()) {
            return false;
        }
        final BlockActivity blockActivity = BlockActivity.getBlockActivity();
        task = blockActivity.getServer().getScheduler().runTaskTimer(blockActivity, this, 0, 20);
        RUNNING = true;
        return true;
    }

    public void stop() {
        task.cancel();
    }

    @Override
    public void run() {
        if (changes.isEmpty()) {
            task.cancel();
            RUNNING = false;
            if (sender != null) sender.sendMessage(ChatColor.GREEN + "Converting finished! (" + progress + "/" + fullQueue + ")");
            return;
        }

        for (int i = 0; i < 1000; i++) {
            final de.diddiz.LogBlock.BlockChange change = changes.poll();
            if (change == null) {
                continue;
            }
            LoggingType logType;

            if (change.replaced == 0 && change.type == 0) {
                continue;
            } else if (change.type == change.replaced) {
                logType = LoggingType.blockinteract;
            } else {
                logType = (change.type == 0 && change.replaced != 0) ? LoggingType.blockbreak : LoggingType.blockplace;
            }

            ExtraData extraData = null;

            if (change.signtext != null) {
                extraData = new BlockExtraData.SignExtraData(change.signtext.split("\0"));
            }

            final BlockActionLog action = new BlockActionLog(logType, change.playerName, change.loc.getWorld(), change.loc.toVector(),
                    change.replaced, change.data, change.type, (logType == LoggingType.blockinteract) ? change.data : (byte) 0, extraData);
            action.setTime(change.date / 1000);
            BlockActivity.sendActionLog(action);
            progress++;
        }

        if (sender != null) sender.sendMessage(ChatColor.GREEN + "Converted " + progress + " logs out of " + fullQueue + "...");
    }

}
