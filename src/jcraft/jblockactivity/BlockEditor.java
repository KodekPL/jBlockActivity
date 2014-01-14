package jcraft.jblockactivity;

import static org.bukkit.Bukkit.getLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import jcraft.jblockactivity.actionlogs.BlockActionLog;
import jcraft.jblockactivity.extradata.BlockExtraData.CommandBlockExtraData;
import jcraft.jblockactivity.extradata.BlockExtraData.FlowerPotExtraData;
import jcraft.jblockactivity.extradata.BlockExtraData.MobSpawnerExtraData;
import jcraft.jblockactivity.extradata.BlockExtraData.SignExtraData;
import jcraft.jblockactivity.extradata.BlockExtraData.SkullExtraData;
import jcraft.jblockactivity.extradata.ExtraData;
import jcraft.jblockactivity.session.LookupCache;
import jcraft.jblockactivity.utils.MaterialNames;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.material.FlowerPot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BlockEditor extends BukkitRunnable {

    private final Queue<BlockChange> edits = new LinkedBlockingQueue<BlockChange>();
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
        return edits.size();
    }

    public int getSuccesses() {
        return successes;
    }

    public int getErrors() {
        return errors.length;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setSender(CommandSender sender) {
        this.sender = sender;
    }

    public void addBlockChange(LoggingType type, String playerName, int x, int y, int z, int oldBlockId, byte oldBlockData, int newBlockId,
            byte newBlockData, ExtraData extraData) {
        edits.add(new BlockChange(type, playerName, world, new Vector(x, y, z), oldBlockId, oldBlockData, newBlockId, newBlockData, extraData));
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
        final float size = edits.size();
        while (!edits.isEmpty() && counter < 100) {
            try {
                switch (edits.poll().perform()) {
                case SUCCESS:
                    successes++;
                    break;
                default:
                    break;
                }
            } catch (final BlockEditorException ex) {
                errorList.add(ex);
            } catch (final Exception ex) {
                getLogger().log(Level.WARNING, "[WorldEditor] Exeption: ", ex);
            }
            counter++;
            if (sender != null) {
                float percentage = ((size - edits.size()) / size) * 100.0F;
                if (percentage % 20 == 0) {
                    sender.sendMessage(ChatColor.GOLD + "[jBlockActivity]" + ChatColor.YELLOW + " Rollback progress: " + percentage + "%"
                            + " Blocks edited: " + counter);
                }
            }
        }
        if (edits.isEmpty()) {
            BlockActivity.getBlockActivity().getServer().getScheduler().cancelTask(taskID);
            errors = errorList.toArray(new BlockEditorException[errorList.size()]);
            this.notify();
        }
    }

    private static enum BlockEditorResult {
        SUCCESS, NO_ACTION;
    }

    private class BlockChange extends BlockActionLog {

        public BlockChange(LoggingType type, String playerName, World world, Vector location, int oldBlockId, byte oldBlockData, int newBlockId,
                byte newBlockData, ExtraData extraData) {
            super(type, playerName, world, location, oldBlockId, oldBlockData, newBlockId, newBlockData, extraData);
        }

        BlockEditorResult perform() throws BlockEditorException {
            if (this.getLogInstance() instanceof BlockActionLog) {
                final BlockActionLog blockLog = (BlockActionLog) this.getLogInstance();
                final Block block = getLocation().getBlock();
                if ((isRedo ? blockLog.getNewBlockId() : blockLog.getOldBlockId()) == 0 && block.getTypeId() == 0) {
                    return BlockEditorResult.NO_ACTION;
                }
                final BlockState state = block.getState();
                if (!world.isChunkLoaded(block.getChunk())) {
                    world.loadChunk(block.getChunk());
                }

                if (state instanceof InventoryHolder) {
                    ((InventoryHolder) state).getInventory().clear();
                    state.update();
                }

                int blockId;
                byte blockData;
                if (!isRedo) {
                    blockId = blockLog.getOldBlockId();
                    blockData = blockLog.getOldBlockData();
                } else {
                    blockId = blockLog.getNewBlockId();
                    blockData = blockLog.getNewBlockData();
                }

                if (block.getTypeId() == blockId) {
                    if (block.getData() != blockData) {
                        block.setData(blockData, true);
                    } else {
                        return BlockEditorResult.NO_ACTION;
                    }
                } else {
                    if (!block.setTypeIdAndData(blockId, blockData, true)) {
                        throw new BlockEditorException(block.getTypeId(), block.getData(), blockId, blockData, block.getLocation());
                    }
                }

                final int currentType = block.getTypeId();
                if (blockLog.getExtraData() != null) {
                    if (currentType == 63 || currentType == 68) {
                        final Sign sign = (Sign) block.getState();
                        final String[] lines = ((SignExtraData) blockLog.getExtraData()).getText();
                        if (lines.length < 4) {
                            return BlockEditorResult.NO_ACTION;
                        }
                        for (int i = 0; i < 4; i++) {
                            sign.setLine(i, lines[i]);
                        }
                        if (!sign.update()) {
                            throw new BlockEditorException("Failed to update signtext of " + MaterialNames.materialName(block.getTypeId()),
                                    block.getLocation());
                        }
                    } else if (currentType == 144) {
                        final Skull skull = (Skull) block.getState();
                        final SkullExtraData data = (SkullExtraData) blockLog.getExtraData();
                        skull.setSkullType(data.getSkullType());
                        skull.setRotation(data.getRotation());
                        if (!data.getName().equals("")) {
                            skull.setOwner(data.getName());
                        }
                        if (!skull.update()) {
                            throw new BlockEditorException("Failed to update skull of " + MaterialNames.materialName(block.getTypeId()),
                                    block.getLocation());
                        }
                    } else if (currentType == 52) {
                        final CreatureSpawner spawner = (CreatureSpawner) block.getState();
                        final MobSpawnerExtraData data = (MobSpawnerExtraData) blockLog.getExtraData();
                        spawner.setSpawnedType(data.getEntityType());
                        if (!spawner.update()) {
                            throw new BlockEditorException("Failed to update mobspawner of " + MaterialNames.materialName(block.getTypeId()),
                                    block.getLocation());
                        }
                    } else if (currentType == 140) {
                        final FlowerPot pot = (FlowerPot) block.getState();
                        final FlowerPotExtraData data = (FlowerPotExtraData) blockLog.getExtraData();
                        pot.setContents(data.getMaterialData());
                        if (!block.getState().update()) {
                            throw new BlockEditorException("Failed to update flower pot of " + MaterialNames.materialName(block.getTypeId()),
                                    block.getLocation());
                        }
                    } else if (currentType == 137) {
                        final CommandBlock commandBlock = (CommandBlock) block.getState();
                        final CommandBlockExtraData data = (CommandBlockExtraData) blockLog.getExtraData();
                        commandBlock.setName(data.getName());
                        commandBlock.setCommand(data.getCommand());
                        if (!commandBlock.update()) {
                            throw new BlockEditorException("Failed to update command block of " + MaterialNames.materialName(block.getTypeId()),
                                    block.getLocation());
                        }
                    }
                }
                return BlockEditorResult.SUCCESS;
            } else {
                return BlockEditorResult.NO_ACTION;
            }

        }

        @Override
        public void executeStatements(Connection connection) throws SQLException {

        }

    }

    @SuppressWarnings("serial")
    public static class BlockEditorException extends Exception implements LookupCache {
        private final Location location;

        public BlockEditorException(int typeBefore, byte dataBefore, int typeAfter, byte dataAfter, Location location) {
            this("Failed to replace " + MaterialNames.materialName(typeBefore, dataBefore) + " with "
                    + MaterialNames.materialName(typeAfter, dataAfter), location);
        }

        public BlockEditorException(String msg, Location location) {
            super(msg + " at " + location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ());
            this.location = location;
        }

        @Override
        public Location getLocation() {
            return location;
        }
    }

}
