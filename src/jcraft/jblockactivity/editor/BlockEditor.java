package jcraft.jblockactivity.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.actionlog.BlockActionLog;
import jcraft.jblockactivity.actionlog.EntityActionLog;
import jcraft.jblockactivity.config.WorldConfig;
import jcraft.jblockactivity.session.LookupCache;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public class BlockEditor extends BukkitRunnable {

    private final Queue<BlockChange> blockEdits = new LinkedBlockingQueue<BlockChange>();
    private final Queue<EntityChange> entityEdits = new LinkedBlockingQueue<EntityChange>();
    private final World world;
    private final WorldConfig worldConfig;
    private final boolean isRedo;
    private CommandSender sender;
    private int taskID;
    private final Map<BlockEditorResult, Integer> results = new HashMap<BlockEditorResult, Integer>();
    private long elapsedTime = 0;
    public LookupCache[] errors;

    public BlockEditor(World world, boolean isRedo) {
        this.world = world;
        this.worldConfig = BlockActivity.getWorldConfig(world.getName());
        this.isRedo = isRedo;
    }

    public int getBlockEditsSize() {
        return blockEdits.size();
    }

    public int getEntityEditsSize() {
        return entityEdits.size();
    }

    public int getSize() {
        return blockEdits.size() + entityEdits.size();
    }

    public int getResults(BlockEditorResult result) {
        return results.containsKey(result) ? results.get(result) : 0;
    }

    public void addResult(BlockEditorResult result) {
        if (results.containsKey(result)) {
            results.put(result, results.get(result) + 1);
        } else {
            results.put(result, 1);
        }
    }

    public int getErrors() {
        return errors.length;
    }

    public boolean isRedo() {
        return isRedo;
    }

    public World getWorld() {
        return world;
    }

    public WorldConfig getWorldConfig() {
        return worldConfig;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setSender(CommandSender sender) {
        this.sender = sender;
    }

    public void addChange(LookupCache log) {
        if (log instanceof BlockActionLog) {
            addBlockChange((BlockActionLog) log);
        } else {
            addEntityChange((EntityActionLog) log);
        }
    }

    public void addBlockChange(BlockActionLog log) {
        blockEdits.add(new BlockChange(log.getLoggingType(), log.getPlayerName(), world, log.getVector(), log.getOldBlockId(), log.getOldBlockData(),
                log.getNewBlockId(), log.getNewBlockData(), log.getExtraData()));
    }

    public void addEntityChange(EntityActionLog log) {
        entityEdits.add(new EntityChange(log.getLoggingType(), log.getPlayerName(), world, log.getVector(), log.getEntityId(), log.getEntityData(),
                log.getExtraData()));
    }

    public synchronized void start() throws Error, InterruptedException {
        final long start = System.currentTimeMillis();
        taskID = BlockActivity.getBlockActivity().getServer().getScheduler().scheduleSyncRepeatingTask(BlockActivity.getBlockActivity(), this, 0, 1);
        if (taskID == -1) {
            throw new Error("Failed to schedule task!");
        }
        try {
            wait();
        } catch (final InterruptedException ex) {
            throw new InterruptedException("Interrupted!");
        }
        elapsedTime = System.currentTimeMillis() - start;
    }

    @Override
    public synchronized void run() {
        final List<BlockEditorException> errorList = new ArrayList<BlockEditorException>();
        int counter = 0;
        float size = blockEdits.size();
        while (!blockEdits.isEmpty() && counter < 100) {
            try {
                addResult(blockEdits.poll().perform(this));
            } catch (final BlockEditorException ex) {
                errorList.add(ex);
            } catch (final Exception ex) {
                Bukkit.getLogger().log(Level.WARNING, "[jBA-BlockEditor] Exeption: ", ex);
            }
            counter++;
            if (sender != null) {
                float percentage = ((size - (blockEdits.size())) / size) * 100.0F;
                if (percentage % 20 == 0) {
                    sender.sendMessage(BlockActivity.prefix + ChatColor.YELLOW + " Rollback progress: " + percentage + "%" + " Blocks edited: "
                            + counter);
                }
            }
        }
        size = entityEdits.size();
        while (!entityEdits.isEmpty() && counter < 100) {
            try {
                addResult(entityEdits.poll().perform(this));
            } catch (final BlockEditorException ex) {
                errorList.add(ex);
            } catch (final Exception ex) {
                Bukkit.getLogger().log(Level.WARNING, "[jBA-BlockEditor] Exeption: ", ex);
            }
            counter++;
            if (sender != null) {
                float percentage = ((size - (entityEdits.size())) / size) * 100.0F;
                if (percentage % 20 == 0) {
                    sender.sendMessage(BlockActivity.prefix + ChatColor.YELLOW + " Rollback progress: " + percentage + "%" + " Entities edited: "
                            + counter);
                }
            }
        }
        if (blockEdits.isEmpty() && entityEdits.isEmpty()) {
            BlockActivity.getBlockActivity().getServer().getScheduler().cancelTask(taskID);
            errors = errorList.toArray(new BlockEditorException[errorList.size()]);
            notifyAll();
        }
    }

}
