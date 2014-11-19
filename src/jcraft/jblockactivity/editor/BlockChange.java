package jcraft.jblockactivity.editor;

import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.actionlog.BlockActionLog;
import jcraft.jblockactivity.extradata.BlockExtraData.CommandBlockExtraData;
import jcraft.jblockactivity.extradata.BlockExtraData.MobSpawnerExtraData;
import jcraft.jblockactivity.extradata.BlockExtraData.SignExtraData;
import jcraft.jblockactivity.extradata.BlockExtraData.SkullExtraData;
import jcraft.jblockactivity.extradata.ExtraData;
import jcraft.jblockactivity.extradata.InventoryExtraData;
import jcraft.jblockactivity.utils.BlocksUtil;
import jcraft.jblockactivity.utils.InventoryUtil;
import jcraft.jblockactivity.utils.MaterialNames;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class BlockChange extends BlockActionLog {

    public BlockChange(LoggingType type, String playerName, World world, Vector location, int oldBlockId, byte oldBlockData, int newBlockId,
            byte newBlockData, ExtraData extraData) {
        super(type, playerName, world, location, oldBlockId, oldBlockData, newBlockId, newBlockData, extraData);
    }

    public BlockEditorResult perform(BlockEditor blockEditor) throws BlockEditorException {
        if (getLogInstance() instanceof BlockActionLog) {
            if (blockEditor.getWorldConfig().blockBlacklist.contains(blockEditor.isRedo() ? getNewBlockId() : getOldBlockId())) {
                return BlockEditorResult.BLOCK_BLACKLIST;
            }

            final Block block = getLocation().getBlock();

            if ((blockEditor.isRedo() ? getNewBlockId() : getOldBlockId()) == 0 && block.getType() == Material.AIR) {
                return BlockEditorResult.NO_BLOCK_ACTION;
            }

            final BlockState state = block.getState();

            if (!blockEditor.getWorld().isChunkLoaded(block.getChunk())) {
                blockEditor.getWorld().loadChunk(block.getChunk());
            }

            int blockId;
            byte blockData;

            if (!blockEditor.isRedo()) {
                blockId = getOldBlockId();
                blockData = getOldBlockData();
            } else {
                blockId = getNewBlockId();
                blockData = getNewBlockData();
            }

            if (getLoggingType() == LoggingType.inventoryaccess) {
                if (getExtraData() != null && !getExtraData().isNull() && BlocksUtil.isContainerBlock(Material.getMaterial(blockId))) {
                    final InventoryExtraData extraData = (InventoryExtraData) getExtraData();

                    for (ItemStack item : extraData.getContent()) {
                        int leftover;

                        try {
                            ItemStack newItem1 = new ItemStack(item.getType(), -item.getAmount(), item.getDurability());
                            newItem1.setItemMeta(item.getItemMeta());
                            leftover = InventoryUtil.modifyContainer(state, newItem1);

                            if (leftover > 0 && (blockId == 54 || blockId == 146)) {
                                for (final BlockFace face : BlocksUtil.PRIMARY_CARDINAL_DIRS) {
                                    if (block.getRelative(face).getTypeId() == blockId) {
                                        ItemStack newItem2 = new ItemStack(item.getType(), (item.getAmount() < 0) ? leftover : -leftover,
                                                item.getDurability());
                                        newItem2.setItemMeta(item.getItemMeta());
                                        leftover = InventoryUtil.modifyContainer(block.getRelative(face).getState(), newItem2);
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
                } else {
                    return BlockEditorResult.NO_INVENTORY_ACTION;
                }
                return BlockEditorResult.INVENTORY_ACCESS;
            }

            if (!(BlocksUtil.isEqualType(block.getTypeId(), blockEditor.isRedo() ? getOldBlockId() : getNewBlockId()) || blockEditor.getWorldConfig().replaceAnyway
                    .contains(block.getTypeId()))) {
                return BlockEditorResult.NO_BLOCK_ACTION;
            }

            if (state instanceof InventoryHolder) {
                ((InventoryHolder) state).getInventory().clear();
                state.update();
            }

            if (block.getTypeId() == blockId) {
                if (block.getData() != blockData) {
                    block.setData(blockData, true);
                } else {
                    return BlockEditorResult.NO_BLOCK_ACTION;
                }
            } else {
                if (!block.setTypeId(blockId)) {
                    throw new BlockEditorException(block.getTypeId(), block.getData(), blockId, blockData, block.getLocation());
                } else {
                    block.setData(blockData, true);
                }
            }

            final int currentType = block.getTypeId();

            if (currentType == 59 || currentType == 104 || currentType == 105 || currentType == 141 || currentType == 142) {
                if (blockEditor.getWorldConfig().farmlandForCrops) {
                    block.getRelative(BlockFace.DOWN).setTypeId(60);
                }
            }

            if (getExtraData() != null && !getExtraData().isNull()) {
                if (currentType == 63 || currentType == 68) {
                    final Sign sign = (Sign) block.getState();
                    final String[] lines = ((SignExtraData) getExtraData()).getText();

                    if (lines == null || lines.length < 4) {
                        return BlockEditorResult.NO_BLOCK_ACTION;
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
                    final SkullExtraData data = (SkullExtraData) getExtraData();

                    skull.setSkullType(data.getSkullType());
                    skull.setRotation(data.getRotation());

                    if (data.getName() != null) {
                        skull.setOwner(data.getName());
                    }

                    if (!skull.update()) {
                        throw new BlockEditorException("Failed to update skull of " + MaterialNames.materialName(block.getTypeId()),
                                block.getLocation());
                    }
                } else if (currentType == 52) {
                    final CreatureSpawner spawner = (CreatureSpawner) block.getState();
                    final MobSpawnerExtraData data = (MobSpawnerExtraData) getExtraData();

                    spawner.setSpawnedType(data.getEntityType());

                    if (!spawner.update()) {
                        throw new BlockEditorException("Failed to update mobspawner of " + MaterialNames.materialName(block.getTypeId()),
                                block.getLocation());
                    }
                } else if (currentType == 137) {
                    final CommandBlock commandBlock = (CommandBlock) block.getState();
                    final CommandBlockExtraData data = (CommandBlockExtraData) getExtraData();

                    if (data.getName() != null) {
                        commandBlock.setName(data.getName());
                    }

                    if (data.getCommand() != null) {
                        commandBlock.setCommand(data.getCommand());
                    }

                    if (!commandBlock.update()) {
                        throw new BlockEditorException("Failed to update command block of " + MaterialNames.materialName(block.getTypeId()),
                                block.getLocation());
                    }
                }
            }

            if (currentType == 0) {
                return BlockEditorResult.BLOCK_REMOVED;
            } else {
                return BlockEditorResult.BLOCK_CREATED;
            }
        } else {
            return BlockEditorResult.NO_BLOCK_ACTION;
        }
    }

}