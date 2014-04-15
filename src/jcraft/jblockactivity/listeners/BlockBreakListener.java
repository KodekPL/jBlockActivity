package jcraft.jblockactivity.listeners;

import static jcraft.jblockactivity.utils.ActivityUtil.canFall;
import static jcraft.jblockactivity.utils.ActivityUtil.getSideRelativeBreakableBlocks;
import static jcraft.jblockactivity.utils.ActivityUtil.isContainerBlock;
import static jcraft.jblockactivity.utils.ActivityUtil.isFallingBlock;
import static jcraft.jblockactivity.utils.ActivityUtil.isFallingBlockKiller;
import static jcraft.jblockactivity.utils.ActivityUtil.isRelativeTopBreakableBlock;

import java.util.List;
import java.util.UUID;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.actionlog.BlockActionLog;
import jcraft.jblockactivity.config.WorldConfig;
import jcraft.jblockactivity.extradata.BlockExtraData;
import jcraft.jblockactivity.extradata.ExtraLoggingTypes.BlockMetaType;
import jcraft.jblockactivity.extradata.InventoryExtraData;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Bed;
import org.bukkit.material.Button;
import org.bukkit.material.CocoaPlant;
import org.bukkit.material.Ladder;
import org.bukkit.material.Lever;
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonBaseMaterial;
import org.bukkit.material.PistonExtensionMaterial;
import org.bukkit.material.RedstoneTorch;
import org.bukkit.material.Torch;
import org.bukkit.material.TrapDoor;
import org.bukkit.material.TripwireHook;

public class BlockBreakListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        final WorldConfig config = BlockActivity.getWorldConfig(event.getPlayer().getWorld().getName());
        if (config == null || !config.isLogging(LoggingType.blockbreak)) {
            return;
        }
        final Block block = event.getBlock();
        final Material material = block.getType();
        final UUID playerUUID = event.getPlayer().getUniqueId();
        final String playerName = event.getPlayer().getName();

        if (BlockActivity.isHidden(playerUUID)) {
            return;
        }

        // TODO: Flower Pot contents
        if (config.isLoggingExtraBlockMeta(BlockMetaType.signtext) && (material == Material.WALL_SIGN || material == Material.SIGN_POST)) {
            /** SIGN **/
            final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, block.getWorld(), block.getLocation()
                    .toVector(), block.getState(), null, BlockExtraData.getExtraData(block.getState()));
            BlockActivity.sendActionLog(action);
        } else if (config.isLoggingExtraBlockMeta(BlockMetaType.skull) && material == Material.SKULL) {
            /** SKULL **/
            final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, block.getWorld(), block.getLocation()
                    .toVector(), block.getState(), null, BlockExtraData.getExtraData(block.getState()));
            BlockActivity.sendActionLog(action);
        } else if (config.isLoggingExtraBlockMeta(BlockMetaType.mobspawner) && material == Material.MOB_SPAWNER) {
            /** MOB SPAWNER **/
            final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, block.getWorld(), block.getLocation()
                    .toVector(), block.getState(), null, BlockExtraData.getExtraData(block.getState()));
            BlockActivity.sendActionLog(action);
        } else if (config.isLoggingExtraBlockMeta(BlockMetaType.commandblock) && material == Material.COMMAND) {
            /** COMMAND BLOCK **/
            final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, block.getWorld(), block.getLocation()
                    .toVector(), block.getState(), null, BlockExtraData.getExtraData(block.getState()));
            BlockActivity.sendActionLog(action);
        } else if (config.isLogging(LoggingType.inventoryaccess) && isContainerBlock(material)) {
            /** CONTAINER **/
            final InventoryExtraData extraData = new InventoryExtraData(((InventoryHolder) block.getState()).getInventory().getContents(), true,
                    block.getWorld());
            final InventoryExtraData emptyExtraData = new InventoryExtraData(new ItemStack[0], false, block.getWorld());
            extraData.compareInventories(emptyExtraData);
            if (!extraData.isEmpty()) {
                final BlockActionLog action = new BlockActionLog(LoggingType.inventoryaccess, playerName, playerUUID, block.getWorld(), block
                        .getLocation().toVector(), block.getState(), block.getState(), extraData);
                BlockActivity.sendActionLog(action);
            }
            final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, block.getWorld(), block.getLocation()
                    .toVector(), block.getState(), null, null);
            BlockActivity.sendActionLog(action);
        } else if (material == Material.ICE) {
            /** ICE **/
            if (event.getPlayer().getGameMode() == GameMode.CREATIVE || event.getPlayer().getItemInHand().containsEnchantment(Enchantment.SILK_TOUCH)) {
                final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, block.getWorld(), block
                        .getLocation().toVector(), block.getState(), null, null);
                BlockActivity.sendActionLog(action);
            } else {
                final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, block.getWorld(), block
                        .getLocation().toVector(), block.getTypeId(), block.getData(), Material.STATIONARY_WATER.getId(), (byte) 0, null);
                BlockActivity.sendActionLog(action);
            }
        } else {
            final BlockActionLog mainaction = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, block.getWorld(), block
                    .getLocation().toVector(), block.getState(), null, null);
            BlockActivity.sendActionLog(mainaction);

            /** DOUBLE BLOCKS **/
            Block checkBlock = null;
            final Block aboveBlock = block.getRelative(BlockFace.UP);
            if (isRelativeTopBreakableBlock(block.getType())) {
                checkBlock = block;
            } else if (isRelativeTopBreakableBlock(aboveBlock.getType())) {
                checkBlock = aboveBlock;
                final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, aboveBlock.getWorld(), aboveBlock
                        .getLocation().toVector(), aboveBlock.getState(), null, null);
                BlockActivity.sendActionLog(action);
            }
            if (checkBlock != null) {
                final Material checkMaterial = checkBlock.getType();

                if (checkMaterial == Material.WOODEN_DOOR || checkMaterial == Material.IRON_DOOR_BLOCK) {
                    /** DOOR **/
                    if (checkBlock.getData() == 8 || checkBlock.getData() == 9) {
                        final BlockState bottomDoor = checkBlock.getRelative(BlockFace.DOWN).getState();
                        if (bottomDoor.getType() == checkMaterial) {
                            final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, bottomDoor.getWorld(),
                                    bottomDoor.getLocation().toVector(), bottomDoor, null, null);
                            BlockActivity.sendActionLog(action);
                        }
                    } else {
                        final BlockState aboveDoor = checkBlock.getRelative(BlockFace.UP).getState();
                        if (aboveDoor.getType() == checkMaterial) {
                            final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, aboveDoor.getWorld(),
                                    aboveDoor.getLocation().toVector(), aboveDoor, null, null);
                            BlockActivity.sendActionLog(action);
                        }
                    }
                } else if (checkMaterial == Material.DOUBLE_PLANT) {
                    /** DOUBLE PLANT **/
                    if (checkBlock.getData() > 5) {
                        final BlockState bottomPart = checkBlock.getRelative(BlockFace.DOWN).getState();
                        if (bottomPart.getType() == checkMaterial) {
                            final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, bottomPart.getWorld(),
                                    bottomPart.getLocation().toVector(), bottomPart, null, null);
                            BlockActivity.sendActionLog(action);
                        }
                    } else {
                        final BlockState abovePart = checkBlock.getRelative(BlockFace.UP).getState();
                        if (abovePart.getType() == checkMaterial) {
                            final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, abovePart.getWorld(),
                                    abovePart.getLocation().toVector(), abovePart, null, null);
                            BlockActivity.sendActionLog(action);
                        }
                    }
                } else if (aboveBlock.getType() == Material.SIGN_POST || aboveBlock.getType() == Material.WALL_SIGN) {
                    /** SIGN **/
                    final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, aboveBlock.getWorld(),
                            aboveBlock.getLocation().toVector(), aboveBlock.getState(), null, BlockExtraData.getExtraData(aboveBlock.getState()));
                    BlockActivity.sendActionLog(action);
                } // TODO: Flower Pot contents
            }

            /** RELATIVE SIDE BLOCKS **/
            final List<Block> relativeBlocks = getSideRelativeBreakableBlocks(block);
            if (!relativeBlocks.isEmpty()) {
                for (Block relativeBlock : relativeBlocks) {
                    final Material relativeMaterial = relativeBlock.getType();
                    final MaterialData materialData = relativeBlock.getState().getData();
                    BlockActionLog action = null;
                    switch (relativeMaterial) {
                    case REDSTONE_TORCH_ON:
                    case REDSTONE_TORCH_OFF:
                        if (relativeBlock.getRelative(((RedstoneTorch) materialData).getAttachedFace()).equals(block)) {
                            action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, relativeBlock.getWorld(), relativeBlock
                                    .getLocation().toVector(), relativeBlock.getState(), null, null);
                        }
                        break;
                    case TORCH:
                        if (relativeBlock.getRelative(((Torch) materialData).getAttachedFace()).equals(block)) {
                            action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, relativeBlock.getWorld(), relativeBlock
                                    .getLocation().toVector(), relativeBlock.getState(), null, null);
                        }
                        break;
                    case COCOA:
                        if (relativeBlock.getRelative(((CocoaPlant) materialData).getAttachedFace()).equals(block)) {
                            action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, relativeBlock.getWorld(), relativeBlock
                                    .getLocation().toVector(), relativeBlock.getState(), null, null);
                        }
                        break;
                    case LADDER:
                        if (relativeBlock.getRelative(((Ladder) materialData).getAttachedFace()).equals(block)) {
                            action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, relativeBlock.getWorld(), relativeBlock
                                    .getLocation().toVector(), relativeBlock.getState(), null, null);
                        }
                        break;
                    case LEVER:
                        if (relativeBlock.getRelative(((Lever) materialData).getAttachedFace()).equals(block)) {
                            action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, relativeBlock.getWorld(), relativeBlock
                                    .getLocation().toVector(), relativeBlock.getState(), null, null);
                        }
                        break;
                    case TRIPWIRE_HOOK:
                        if (relativeBlock.getRelative(((TripwireHook) materialData).getAttachedFace()).equals(block)) {
                            action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, relativeBlock.getWorld(), relativeBlock
                                    .getLocation().toVector(), relativeBlock.getState(), null, null);
                        }
                        break;
                    case WOOD_BUTTON:
                    case STONE_BUTTON:
                        if (relativeBlock.getRelative(((Button) materialData).getAttachedFace()).equals(block)) {
                            action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, relativeBlock.getWorld(), relativeBlock
                                    .getLocation().toVector(), relativeBlock.getState(), null, null);
                        }
                        break;
                    case WALL_SIGN:
                        if (relativeBlock.getRelative(((org.bukkit.material.Sign) materialData).getAttachedFace()).equals(block)) {
                            action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, relativeBlock.getWorld(), relativeBlock
                                    .getLocation().toVector(), relativeBlock.getState(), null, BlockExtraData.getExtraData(relativeBlock.getState()));
                        }
                        break;
                    case TRAP_DOOR:
                        if (relativeBlock.getRelative(((TrapDoor) materialData).getAttachedFace()).equals(block)) {
                            action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, relativeBlock.getWorld(), relativeBlock
                                    .getLocation().toVector(), relativeBlock.getState(), null, null);
                        }
                        break;
                    case BED_BLOCK:
                        Bed bed = (Bed) materialData;
                        if (bed.isHeadOfBed()) {
                            if (relativeBlock.getRelative(bed.getFacing().getOppositeFace()).getType() == Material.BED_BLOCK) {
                                action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, relativeBlock.getWorld(), relativeBlock
                                        .getLocation().toVector(), relativeBlock.getState(), null, null);
                            }
                        } else {
                            if (relativeBlock.getRelative(bed.getFacing()).getType() == Material.BED_BLOCK) {
                                action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, relativeBlock.getWorld(), relativeBlock
                                        .getLocation().toVector(), relativeBlock.getState(), null, null);
                            }
                        }
                        break;
                    case PISTON_BASE:
                    case PISTON_STICKY_BASE:
                        PistonBaseMaterial piston = (PistonBaseMaterial) materialData;
                        if (piston.isPowered()) {
                            if (relativeBlock.getRelative(piston.getFacing()).getType() == Material.PISTON_EXTENSION) {
                                action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, relativeBlock.getWorld(), relativeBlock
                                        .getLocation().toVector(), relativeBlock.getState(), null, null);
                            }
                        }
                        break;
                    case PISTON_EXTENSION:
                        PistonExtensionMaterial extension = (PistonExtensionMaterial) materialData;
                        if (relativeBlock.getRelative(extension.getAttachedFace()).getType() == Material.PISTON_EXTENSION) {
                            action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, relativeBlock.getWorld(), relativeBlock
                                    .getLocation().toVector(), relativeBlock.getState(), null, null);
                        }
                        break;
                    default:
                        action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, relativeBlock.getWorld(), relativeBlock
                                .getLocation().toVector(), relativeBlock.getState(), null, null);
                        break;
                    }
                    BlockActivity.sendActionLog(action);
                }
            }
        }

        /** FALLING BLOCKS **/
        Block fallingBlock = block.getRelative(BlockFace.UP);
        int up = 0;
        final int highestBlock = fallingBlock.getWorld().getHighestBlockYAt(fallingBlock.getLocation());
        while (isFallingBlock(fallingBlock.getType())) {

            BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, fallingBlock.getWorld(), fallingBlock
                    .getLocation().toVector(), fallingBlock.getState(), null, null);
            BlockActivity.sendActionLog(action);

            // The potential postion of the block after fall (explosions can affect position)
            int x = block.getX();
            int y = block.getY();
            int z = block.getZ();
            while (y > 0 && canFall(block.getWorld(), x, (y - 1), z)) {
                y--;
            }

            if (y > 0) {
                Location finalLocation = new Location(block.getWorld(), x, y, z);
                Block finalBlock = finalLocation.getBlock();

                if (!isFallingBlockKiller(finalBlock.getRelative(BlockFace.DOWN).getType())) {
                    finalLocation.add(0, up, 0);
                    if (finalBlock.getType() == Material.AIR || isFallingBlock(finalBlock.getType())) {
                        BlockActionLog action1 = new BlockActionLog(LoggingType.blockplace, playerName, playerUUID, finalLocation.getWorld(),
                                finalLocation.toVector(), null, fallingBlock.getState(), null);
                        BlockActivity.sendActionLog(action1);
                    } else {
                        BlockActionLog action1 = new BlockActionLog(LoggingType.blockplace, playerName, playerUUID, finalLocation.getWorld(),
                                finalLocation.toVector(), finalBlock.getState(), fallingBlock.getState(), null);
                        BlockActivity.sendActionLog(action1);
                    }
                    up++;
                }
            }
            if (fallingBlock.getY() >= highestBlock) {
                break;
            }
            fallingBlock = fallingBlock.getRelative(BlockFace.UP);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBucketFill(PlayerBucketFillEvent event) {
        final Block block = event.getBlockClicked();
        final WorldConfig config = BlockActivity.getWorldConfig(block.getWorld().getName());
        if (config == null || !config.isLogging(LoggingType.blockbreak)) {
            return;
        }
        final UUID playerUUID = event.getPlayer().getUniqueId();
        final String playerName = event.getPlayer().getName();

        if (BlockActivity.isHidden(playerUUID)) {
            return;
        }

        final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, block.getWorld(), block.getLocation()
                .toVector(), block.getState(), null, null);
        BlockActivity.sendActionLog(action);
    }

}
