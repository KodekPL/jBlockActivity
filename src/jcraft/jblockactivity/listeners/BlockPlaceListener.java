package jcraft.jblockactivity.listeners;

import java.util.List;
import java.util.UUID;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.actionlog.BlockActionLog;
import jcraft.jblockactivity.config.WorldConfig;
import jcraft.jblockactivity.extradata.BlockExtraData;
import jcraft.jblockactivity.extradata.BlockExtraData.SignExtraData;
import jcraft.jblockactivity.extradata.BlockExtraData.SkullExtraData;
import jcraft.jblockactivity.extradata.ExtraLoggingTypes.BlockMetaType;
import jcraft.jblockactivity.utils.BlocksUtil;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.material.Bed;

public class BlockPlaceListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final WorldConfig config = BlockActivity.getWorldConfig(event.getPlayer().getWorld().getName());

        if (config == null || !config.isLogging(LoggingType.blockplace)) {
            return;
        }
        final Block block = event.getBlock();
        final Material material = block.getType();

        if (config.isLoggingExtraBlockMeta(BlockMetaType.signtext) && (material == Material.SIGN_POST || material == Material.WALL_SIGN)) {
            // SignChangeEvent
            return;
        }

        final BlockState beforeState = event.getBlockReplacedState();
        final BlockState afterState = event.getBlockPlaced().getState();
        final UUID playerUUID = event.getPlayer().getUniqueId();
        final String playerName = event.getPlayer().getName();

        if (BlockActivity.isHidden(playerUUID)) {
            return;
        }

        /** FALLING BLOCK **/
        if (BlocksUtil.isFallingBlock(material)) {
            if (beforeState.getType() != Material.AIR) {
                final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, afterState.getWorld(), afterState
                        .getLocation().toVector(), beforeState, afterState, null);
                BlockActivity.sendActionLog(action);
            }

            final int x = block.getX();
            int y = block.getY();
            final int z = block.getZ();

            if (event.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) {
                while (y > 0 && BlocksUtil.isFallingOverridingBlock(block.getWorld().getBlockAt(x, (y - 1), z).getType())) {
                    y--;
                }
            }

            if (y > 0) {
                final Location finalLocation = new Location(block.getWorld(), x, y, z);
                final Block finalBlock = finalLocation.getBlock();

                if (BlocksUtil.isFallingBlockKiller(finalBlock.getRelative(BlockFace.DOWN).getType())) {
                    return;
                }

                if (finalBlock.getType() == Material.AIR || finalLocation.equals(block.getLocation())) {
                    final BlockActionLog action = new BlockActionLog(LoggingType.blockplace, playerName, playerUUID, finalLocation.getWorld(),
                            finalLocation.toVector(), null, block.getState(), null);

                    BlockActivity.sendActionLog(action);
                } else {
                    final BlockActionLog action = new BlockActionLog(LoggingType.blockplace, playerName, playerUUID, finalLocation.getWorld(),
                            finalLocation.toVector(), finalBlock.getState(), block.getState(), null);

                    BlockActivity.sendActionLog(action);
                }
            }
            return;
        }

        final boolean delayLog = (afterState.getType() == Material.SKULL);

        if (delayLog) {
            BlockActivity.getBlockActivity().getServer().getScheduler().scheduleSyncDelayedTask(BlockActivity.getBlockActivity(), new Runnable() {
                @Override
                public void run() {
                    sendLogaction(event);
                }
            }, 1L);
        } else {
            sendLogaction(event);
        }
    }

    private void sendLogaction(BlockPlaceEvent event) {
        final Block block = event.getBlock();
        final Material material = block.getType();
        final BlockState beforeState = event.getBlockReplacedState();
        final BlockState afterState = event.getBlockPlaced().getState();
        final UUID playerUUID = event.getPlayer().getUniqueId();
        final String playerName = event.getPlayer().getName();

        BlockExtraData extraData = null;

        if (afterState.getType() == Material.SKULL) {
            final Skull skull = (Skull) afterState;
            extraData = new SkullExtraData(skull.getRotation(), skull.getOwner(), skull.getSkullType());
        }

        if (beforeState.getType() == Material.AIR) {
            final BlockActionLog action = new BlockActionLog(LoggingType.blockplace, playerName, playerUUID, afterState.getWorld(), afterState
                    .getLocation().toVector(), null, afterState, extraData);

            BlockActivity.sendActionLog(action);
        } else {
            final BlockActionLog action = new BlockActionLog(LoggingType.blockplace, playerName, playerUUID, afterState.getWorld(), afterState
                    .getLocation().toVector(), beforeState, block.getState(), extraData);

            BlockActivity.sendActionLog(action);
        }

        if (event instanceof BlockMultiPlaceEvent) {
            BlockActionLog action = null;

            if (material == Material.WOODEN_DOOR || material == Material.IRON_DOOR_BLOCK) {
                if (afterState.getRawData() <= 3) {
                    final BlockFace doorFace = BlocksUtil.yawToFace(event.getPlayer().getEyeLocation().getYaw()).getOppositeFace();
                    final boolean doubleDoor = afterState.getBlock().getRelative(BlocksUtil.turnFace(doorFace, false)).getType() == afterState
                            .getType();

                    action = new BlockActionLog(LoggingType.blockplace, playerName, playerUUID, afterState.getWorld(), afterState.getLocation()
                            .add(0, 1, 0).toVector(), 0, (byte) 0, material.getId(), doubleDoor ? (byte) 9 : (byte) 8, null);
                }
            } else if (material == Material.BED_BLOCK) {
                final Bed bed = (Bed) afterState.getData();

                if (bed.getData() <= 3) {
                    action = new BlockActionLog(LoggingType.blockplace, playerName, playerUUID, afterState.getWorld(), afterState.getBlock()
                            .getRelative(bed.getFacing()).getLocation().toVector(), 0, (byte) 0, material.getId(), (byte) (bed.getData() + 8), null);
                }
            } else if (material == Material.DOUBLE_PLANT) {
                if (afterState.getRawData() <= 5) {
                    action = new BlockActionLog(LoggingType.blockplace, playerName, playerUUID, afterState.getWorld(), afterState.getLocation()
                            .add(0, 1, 0).toVector(), 0, (byte) 0, material.getId(), (byte) 11, null);
                }
            } else {
                // Handle custom multi blocks
                final BlockMultiPlaceEvent multiEvent = (BlockMultiPlaceEvent) event;
                final List<BlockState> blocks = multiEvent.getReplacedBlockStates();

                BlockActivity.getBlockActivity().getServer().getScheduler().scheduleSyncDelayedTask(BlockActivity.getBlockActivity(), new Runnable() {
                    @Override
                    public void run() {
                        for (BlockState block : blocks) {
                            if (block.getLocation().getBlock().getType() != block.getType()) {
                                BlockActivity.sendActionLog(new BlockActionLog(LoggingType.blockplace, playerName, playerUUID, block.getWorld(),
                                        block.getLocation().toVector(), block, block.getLocation().getBlock().getState(), null));
                            }
                        }
                    }
                }, 1L);
                return;
            }

            BlockActivity.sendActionLog(action);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        final Block block = event.getBlockClicked().getRelative(event.getBlockFace());
        final WorldConfig config = BlockActivity.getWorldConfig(block.getWorld().getName());

        if (config == null || !config.isLogging(LoggingType.blockplace)) {
            return;
        }

        final UUID playerUUID = event.getPlayer().getUniqueId();
        final String playerName = event.getPlayer().getName();

        if (BlockActivity.isHidden(playerUUID)) {
            return;
        }

        Material material;

        if (event.getBucket() == Material.WATER_BUCKET) {
            material = Material.STATIONARY_WATER;
        } else {
            material = Material.STATIONARY_LAVA;
        }

        final BlockActionLog action = new BlockActionLog(LoggingType.blockplace, playerName, playerUUID, block.getWorld(), block.getLocation()
                .toVector(), 0, (byte) 0, material.getId(), (byte) 0, null);

        BlockActivity.sendActionLog(action);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        final Block block = event.getBlock();
        final WorldConfig config = BlockActivity.getWorldConfig(block.getWorld().getName());

        if (config == null || !config.isLogging(LoggingType.blockplace)) {
            return;
        }

        if (!config.isLoggingExtraBlockMeta(BlockMetaType.signtext)) {
            return;
        }

        final UUID playerUUID = event.getPlayer().getUniqueId();
        final String playerName = event.getPlayer().getName();

        if (BlockActivity.isHidden(playerUUID)) {
            return;
        }

        final SignExtraData extraData = new SignExtraData(event.getLines());
        final BlockActionLog action = new BlockActionLog(LoggingType.blockplace, playerName, playerUUID, block.getWorld(), block.getLocation()
                .toVector(), null, block.getState(), extraData);

        BlockActivity.sendActionLog(action);
    }

}
