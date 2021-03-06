package jcraft.jblockactivity.listeners;

import java.util.List;
import java.util.UUID;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.actionlog.BlockActionLog;
import jcraft.jblockactivity.config.WorldConfig;
import jcraft.jblockactivity.extradata.BlockExtraData;
import jcraft.jblockactivity.extradata.ExtraLoggingTypes.BlockMetaType;
import jcraft.jblockactivity.extradata.InventoryExtraData;
import jcraft.jblockactivity.utils.BlocksUtil;

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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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

        if (material == Material.ICE) {
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
            return;
        }

        blockBreak(config, block, material, playerName, playerUUID);
    }

    public static void blockBreak(WorldConfig config, Block block, Material material, String playerName, UUID playerUUID) {
        blockBreak(config, block, material, playerName, playerUUID, false);
    }

    public static void blockBreak(WorldConfig config, Block block, Material material, String playerName, UUID playerUUID, boolean simple) {
        // TODO: Flower Pot contents
        if ((material == Material.WALL_SIGN || material == Material.SIGN_POST) && config.isLoggingExtraBlockMeta(BlockMetaType.signtext)) {
            /** SIGN **/
            final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, block.getWorld(), block.getLocation()
                    .toVector(), block.getState(), null, BlockExtraData.getExtraData(block.getState()));

            BlockActivity.sendActionLog(action);
        } else if (material == Material.SKULL && config.isLoggingExtraBlockMeta(BlockMetaType.skull)) {
            /** SKULL **/
            final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, block.getWorld(), block.getLocation()
                    .toVector(), block.getState(), null, BlockExtraData.getExtraData(block.getState()));

            BlockActivity.sendActionLog(action);
        } else if (material == Material.MOB_SPAWNER && config.isLoggingExtraBlockMeta(BlockMetaType.mobspawner)) {
            /** MOB SPAWNER **/
            final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, block.getWorld(), block.getLocation()
                    .toVector(), block.getState(), null, BlockExtraData.getExtraData(block.getState()));

            BlockActivity.sendActionLog(action);
        } else if (material == Material.COMMAND && config.isLoggingExtraBlockMeta(BlockMetaType.commandblock)) {
            /** COMMAND BLOCK **/
            final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, block.getWorld(), block.getLocation()
                    .toVector(), block.getState(), null, BlockExtraData.getExtraData(block.getState()));

            BlockActivity.sendActionLog(action);
        } else if (BlocksUtil.isContainerBlock(material) && config.isLogging(LoggingType.inventoryaccess)) {
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
        } else {
            final BlockActionLog mainAction = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, block.getWorld(), block
                    .getLocation().toVector(), block.getState(), null, null);

            BlockActivity.sendActionLog(mainAction);
        }

        if (simple) {
            // Skip other checkings if logging simple block break
            return;
        }

        /** DOUBLE BLOCKS **/
        Block doubleBlock = null;
        if (BlocksUtil.isBottomRelativeBreakableBlock(block.getType())) {
            doubleBlock = block;
        } else if (BlocksUtil.isBottomRelativeBreakableBlock(block.getRelative(BlockFace.UP).getType())) {
            doubleBlock = block.getRelative(BlockFace.UP);

            final BlockActionLog upperPartAction = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, doubleBlock.getWorld(),
                    doubleBlock.getLocation().toVector(), doubleBlock.getState(), null, BlockExtraData.getExtraData(doubleBlock.getState()));

            BlockActivity.sendActionLog(upperPartAction);
        }

        if (doubleBlock != null) {
            if (doubleBlock.getType() == Material.WOODEN_DOOR || doubleBlock.getType() == Material.IRON_DOOR_BLOCK) {
                /** DOOR **/
                if (doubleBlock.getData() == 8 || doubleBlock.getData() == 9) {
                    final BlockState bottomDoor = doubleBlock.getRelative(BlockFace.DOWN).getState();

                    if (bottomDoor.getType() == doubleBlock.getType()) {
                        final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, bottomDoor.getWorld(),
                                bottomDoor.getLocation().toVector(), bottomDoor, null, null);

                        BlockActivity.sendActionLog(action);
                    }
                } else {
                    final BlockState aboveDoor = doubleBlock.getRelative(BlockFace.UP).getState();

                    if (aboveDoor.getType() == doubleBlock.getType()) {
                        final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, aboveDoor.getWorld(),
                                aboveDoor.getLocation().toVector(), aboveDoor, null, null);

                        BlockActivity.sendActionLog(action);
                    }
                }
            } else if (doubleBlock.getType() == Material.DOUBLE_PLANT) {
                /** DOUBLE PLANT **/
                if (doubleBlock.getData() > 5) {
                    final BlockState bottomPart = doubleBlock.getRelative(BlockFace.DOWN).getState();
                    if (bottomPart.getType() == doubleBlock.getType()) {
                        final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, bottomPart.getWorld(),
                                bottomPart.getLocation().toVector(), bottomPart, null, null);

                        BlockActivity.sendActionLog(action);
                    }
                } else {
                    final BlockState abovePart = doubleBlock.getRelative(BlockFace.UP).getState();
                    if (abovePart.getType() == doubleBlock.getType()) {
                        final BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, abovePart.getWorld(),
                                abovePart.getLocation().toVector(), abovePart, null, null);

                        BlockActivity.sendActionLog(action);
                    }
                }
            }
            // TODO: Flower Pot contents
        }

        /** RELATIVE SIDE BLOCKS **/
        final List<Block> relativeBlocks = BlocksUtil.getSideRelativeBreakableBlocks(block);

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
                    action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, relativeBlock.getWorld(), relativeBlock.getLocation()
                            .toVector(), relativeBlock.getState(), null, null);
                    break;
                }
                BlockActivity.sendActionLog(action);
            }
        }

        /** FALLING BLOCKS **/
        Block fallingBlock = block.getRelative(BlockFace.UP);

        int up = 0;

        final int highestBlock = fallingBlock.getWorld().getHighestBlockYAt(fallingBlock.getLocation());

        while (BlocksUtil.isFallingBlock(fallingBlock.getType())) {
            BlockActionLog action = new BlockActionLog(LoggingType.blockbreak, playerName, playerUUID, fallingBlock.getWorld(), fallingBlock
                    .getLocation().toVector(), fallingBlock.getState(), null, null);

            BlockActivity.sendActionLog(action);

            // The potential postion of the block after fall (explosions can affect position)
            int x = block.getX();
            int y = block.getY();
            int z = block.getZ();

            while (y > 0 && BlocksUtil.isFallingOverridingBlock(block.getWorld().getBlockAt(x, (y - 1), z).getType())) {
                y--;
            }

            if (y > 0) {
                Location finalLocation = new Location(block.getWorld(), x, y, z);
                Block finalBlock = finalLocation.getBlock();

                if (!BlocksUtil.isFallingBlockKiller(finalBlock.getRelative(BlockFace.DOWN).getType())) {
                    finalLocation.add(0, up, 0);

                    if (finalBlock.getType() == Material.AIR || BlocksUtil.isFallingBlock(finalBlock.getType())) {
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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
