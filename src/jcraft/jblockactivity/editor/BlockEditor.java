package jcraft.jblockactivity.editor;

import static org.bukkit.Bukkit.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.actionlogs.BlockActionLog;
import jcraft.jblockactivity.actionlogs.EntityActionLog;
import jcraft.jblockactivity.session.LookupCache;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public class BlockEditor extends BukkitRunnable {

    private final Queue<BlockChange> blockEdits = new LinkedBlockingQueue<BlockChange>();
    private final Queue<EntityChange> entityEdits = new LinkedBlockingQueue<EntityChange>();
    private final World world;
    private final boolean isRedo;
    private CommandSender sender;
    private int taskID;
    private int successes = 0;
    private long elapsedTime = 0;
    public LookupCache[] errors;

    public BlockEditor(World world, boolean isRedo) {
        this.world = world;
        this.isRedo = isRedo;
    }

    public int getSize() {
        return blockEdits.size() + entityEdits.size();
    }

    public int getSuccesses() {
        return successes;
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
            this.wait();
        } catch (final InterruptedException ex) {
            throw new InterruptedException("Interrupted!");
        }
        elapsedTime = System.currentTimeMillis() - start;
    }

    @Override
    public synchronized void run() {
        final List<BlockEditorException> errorList = new ArrayList<BlockEditorException>();
        int counter = 0;
        final float size = blockEdits.size() + entityEdits.size();
        while (!blockEdits.isEmpty() && counter < 100) {
            try {
                switch (blockEdits.poll().perform(this)) {
                case SUCCESS:
                    successes++;
                    break;
                default:
                    break;
                }
            } catch (final BlockEditorException ex) {
                errorList.add(ex);
            } catch (final Exception ex) {
                getLogger().log(Level.WARNING, "[BlockEditor] Exeption: ", ex);
            }
            counter++;
            if (sender != null) {
                float percentage = ((size - (blockEdits.size() - entityEdits.size())) / size) * 100.0F;
                if (percentage % 20 == 0) {
                    sender.sendMessage(BlockActivity.prefix + ChatColor.YELLOW + " Rollback progress: " + percentage + "%" + " Blocks edited: "
                            + counter);
                }
            }
        }
        while (!entityEdits.isEmpty() && counter < 100) {
            try {
                switch (entityEdits.poll().perform(this)) {
                case SUCCESS:
                    successes++;
                    break;
                default:
                    break;
                }
            } catch (final BlockEditorException ex) {
                errorList.add(ex);
            } catch (final Exception ex) {
                getLogger().log(Level.WARNING, "[BlockEditor] Exeption: ", ex);
            }
            counter++;
            if (sender != null) {
                float percentage = ((size - (blockEdits.size() - entityEdits.size())) / size) * 100.0F;
                if (percentage % 20 == 0) {
                    sender.sendMessage(BlockActivity.prefix + ChatColor.YELLOW + " Rollback progress: " + percentage + "%" + " Blocks edited: "
                            + counter);
                }
            }
        }
        if (blockEdits.isEmpty() && entityEdits.isEmpty()) {
            BlockActivity.getBlockActivity().getServer().getScheduler().cancelTask(taskID);
            errors = errorList.toArray(new BlockEditorException[errorList.size()]);
            this.notifyAll();
        }
    }

}
