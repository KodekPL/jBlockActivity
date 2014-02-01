package jcraft.jblockactivity;

import static jcraft.jblockactivity.utils.ActivityUtil.isContainerBlock;
import static jcraft.jblockactivity.utils.ActivityUtil.isEqualType;
import static jcraft.jblockactivity.utils.ActivityUtil.isSameLocation;
import static jcraft.jblockactivity.utils.ActivityUtil.modifyContainer;
import static jcraft.jblockactivity.utils.ActivityUtil.primaryCardinalDirs;
import static org.bukkit.Bukkit.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import jcraft.jblockactivity.actionlogs.BlockActionLog;
import jcraft.jblockactivity.actionlogs.EntityActionLog;
import jcraft.jblockactivity.extradata.BlockExtraData.CommandBlockExtraData;
import jcraft.jblockactivity.extradata.BlockExtraData.FlowerPotExtraData;
import jcraft.jblockactivity.extradata.BlockExtraData.MobSpawnerExtraData;
import jcraft.jblockactivity.extradata.BlockExtraData.SignExtraData;
import jcraft.jblockactivity.extradata.BlockExtraData.SkullExtraData;
import jcraft.jblockactivity.extradata.ExtraData;
import jcraft.jblockactivity.extradata.InventoryExtraData;
import jcraft.jblockactivity.session.LookupCache;
import jcraft.jblockactivity.utils.MaterialNames;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.FlowerPot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

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
                switch (blockEdits.poll().perform()) {
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
                switch (entityEdits.poll().perform()) {
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

    private static enum BlockEditorResult {
        SUCCESS, NO_ACTION, BLACKLIST;
    }

    private class EntityChange extends EntityActionLog {

        public EntityChange(LoggingType type, String playerName, World world, Vector location, int entityId, int dataId, ExtraData extraData) {
            super(type, playerName, world, location, entityId, dataId, extraData);
        }

        BlockEditorResult perform() throws BlockEditorException {
            if (this.getLogInstance() instanceof EntityActionLog) {
                final Block block = getLocation().getBlock();
                final Chunk chunk = block.getChunk();
                if (getLoggingType() == LoggingType.hangingbreak || getLoggingType() == LoggingType.hangingplace) {
                    final Hanging[] hangings = getHangings(chunk, getLocation().toVector(), getEntityId());
                    final BlockFace face = BlockFace.values()[getEntityData()];
                    final Block hangBlock = block.getRelative(face.getOppositeFace());

                    if (getLoggingType() == LoggingType.hangingbreak && !isRedo) {
                        for (Hanging hanging : hangings) {
                            if (hanging.getFacing().ordinal() == getEntityData()) {
                                return BlockEditorResult.NO_ACTION;
                            }
                        }
                        if (!block.getType().isTransparent()) {
                            throw new BlockEditorException("No space to place " + MaterialNames.entityName(getEntityId()), block.getLocation());
                        }

                        try {
                            final Hanging hanging = (Hanging) getWorld().spawn(hangBlock.getLocation(),
                                    EntityType.fromId(getEntityId()).getEntityClass());
                            hanging.teleport(block.getLocation());
                            // BUG: https://bukkit.atlassian.net/browse/BUKKIT-3371
                            hanging.setFacingDirection(face, true);
                        } catch (IllegalArgumentException e) {
                            throw new BlockEditorException("Invalid hanging block to place " + MaterialNames.entityName(getEntityId()),
                                    block.getLocation());
                        }
                    } else {
                        for (Hanging hanging : hangings) {
                            if (hanging.getFacing().ordinal() == getEntityData()) {
                                hanging.remove();
                                return BlockEditorResult.SUCCESS;
                            }
                        }
                        return BlockEditorResult.NO_ACTION;
                    }
                }
                return BlockEditorResult.SUCCESS;
            } else {
                return BlockEditorResult.NO_ACTION;
            }
        }

        private Hanging[] getHangings(Chunk chunk, Vector vector, int entityId) {
            final List<Entity> hangings = new ArrayList<Entity>();
            for (Entity entity : chunk.getEntities()) {
                if (entity instanceof Hanging && entity.getType().getTypeId() == entityId) {
                    if (isSameLocation(entity.getLocation().toVector(), vector)) {
                        hangings.add(entity);
                    }
                }
            }
            return hangings.toArray(new Hanging[hangings.size()]);
        }

    }

    private class BlockChange extends BlockActionLog {

        public BlockChange(LoggingType type, String playerName, World world, Vector location, int oldBlockId, byte oldBlockData, int newBlockId,
                byte newBlockData, ExtraData extraData) {
            super(type, playerName, world, location, oldBlockId, oldBlockData, newBlockId, newBlockData, extraData);
        }

        BlockEditorResult perform() throws BlockEditorException {
            if (this.getLogInstance() instanceof BlockActionLog) {
                final BlockActionLog blockLog = (BlockActionLog) this.getLogInstance();
                if (BlockActivity.config.blockBlacklist.contains(isRedo ? blockLog.getNewBlockId() : blockLog.getOldBlockId())) {
                    return BlockEditorResult.BLACKLIST;
                }

                final Block block = getLocation().getBlock();
                if ((isRedo ? blockLog.getNewBlockId() : blockLog.getOldBlockId()) == 0 && block.getType() == Material.AIR) {
                    return BlockEditorResult.NO_ACTION;
                }
                final BlockState state = block.getState();
                if (!world.isChunkLoaded(block.getChunk())) {
                    world.loadChunk(block.getChunk());
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

                if (getLoggingType() == LoggingType.inventoryaccess) {
                    if (blockLog.getExtraData() != null) {
                        if (isContainerBlock(Material.getMaterial(blockId))) {
                            final InventoryExtraData extraData = (InventoryExtraData) blockLog.getExtraData();
                            for (ItemStack item : extraData.getContent()) {
                                int leftover;
                                try {
                                    leftover = modifyContainer(state, new ItemStack(item.getType(), -item.getAmount(), item.getDurability()));
                                    if (leftover > 0 && (blockId == 54 || blockId == 146)) {
                                        for (final BlockFace face : primaryCardinalDirs) {
                                            if (block.getRelative(face).getTypeId() == blockId) {
                                                leftover = modifyContainer(
                                                        block.getRelative(face).getState(),
                                                        new ItemStack(item.getType(), (item.getAmount() < 0) ? leftover : -leftover, item
                                                                .getDurability()));
                                                break;
                                            }
                                        }
                                    }
                                } catch (final Exception ex) {
                                    throw new BlockEditorException(ex.getMessage(), block.getLocation());
                                }
                                if (leftover > 0 && item.getAmount() < 0) {
                                    throw new BlockEditorException("Not enough space left in " + MaterialNames.materialName(block.getTypeId()),
                                            block.getLocation());
                                }
                            }

                            if (!state.update()) {
                                throw new BlockEditorException("Failed to update inventory of "
                                        + MaterialNames.materialName(block.getTypeId(), block.getData()), block.getLocation());
                            }
                        }
                    } else {
                        return BlockEditorResult.NO_ACTION;
                    }
                    return BlockEditorResult.SUCCESS;
                }

                if (!(isEqualType(block.getTypeId(), isRedo ? blockLog.getOldBlockId() : blockLog.getNewBlockId()) || BlockActivity.config.replaceAnyway
                        .contains(block.getTypeId()))) {
                    return BlockEditorResult.NO_ACTION;
                }

                if (state instanceof InventoryHolder) {
                    ((InventoryHolder) state).getInventory().clear();
                    state.update();
                }

                if (block.getTypeId() == blockId) {
                    if (block.getData() != blockData) {
                        block.setData(blockData, true);
                    } else {
                        return BlockEditorResult.NO_ACTION;
                    }
                } else {
                    if (!block.setTypeId(blockId)) {
                        throw new BlockEditorException(block.getTypeId(), block.getData(), blockId, blockData, block.getLocation());
                    } else {
                        block.setData(blockData, true);
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

    }

    public static class BlockEditorException extends Exception implements LookupCache {
        private static final long serialVersionUID = 2182077864822995737L;
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
