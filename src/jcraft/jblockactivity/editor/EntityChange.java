package jcraft.jblockactivity.editor;

import static jcraft.jblockactivity.utils.ActivityUtil.isItemSimilar;
import static jcraft.jblockactivity.utils.ActivityUtil.isSameLocation;

import java.util.ArrayList;
import java.util.List;

import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.actionlogs.EntityActionLog;
import jcraft.jblockactivity.extradata.ExtraData;
import jcraft.jblockactivity.extradata.InventoryExtraData;
import jcraft.jblockactivity.utils.MaterialNames;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class EntityChange extends EntityActionLog {

    public EntityChange(LoggingType type, String playerName, World world, Vector location, int entityId, int dataId, ExtraData extraData) {
        super(type, playerName, world, location, entityId, dataId, extraData);
    }

    BlockEditorResult perform(BlockEditor blockEditor) throws BlockEditorException {
        if (this.getLogInstance() instanceof EntityActionLog) {
            final Block block = getLocation().getBlock();
            final Chunk chunk = block.getChunk();
            if (getLoggingType() == LoggingType.hangingbreak || getLoggingType() == LoggingType.hangingplace) {
                final Hanging[] hangings = getHangings(chunk, getLocation().toVector(), getEntityId());
                final BlockFace face = BlockFace.values()[getEntityData()];
                final Block hangBlock = block.getRelative(face.getOppositeFace());

                if (getLoggingType() == LoggingType.hangingbreak && !blockEditor.isRedo()) {
                    for (Hanging hanging : hangings) {
                        if (hanging.getFacing().ordinal() == getEntityData()) {
                            return BlockEditorResult.NO_ACTION;
                        }
                    }
                    if (!block.getType().isTransparent()) {
                        throw new BlockEditorException("No space to place " + MaterialNames.entityName(getEntityId()), block.getLocation());
                    }

                    try {
                        final Hanging hanging = (Hanging) getWorld()
                                .spawn(hangBlock.getLocation(), EntityType.fromId(getEntityId()).getEntityClass());
                        hanging.teleport(block.getLocation());
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
            } else if (getLoggingType() == LoggingType.hanginginteract) {
                // Only for item frames for now (if Mojang will add some kind of new item frame)
                if (getExtraData() != null) {
                    final Hanging[] hangings = getHangings(chunk, getLocation().toVector(), getEntityId());
                    final InventoryExtraData extraData = (InventoryExtraData) getExtraData();
                    if (extraData.getContent().length == 0) {
                        return BlockEditorResult.NO_ACTION;
                    }
                    final ItemStack item = extraData.getContent()[0];

                    if (item.getAmount() < 0 && !blockEditor.isRedo()) {
                        ItemFrame firstEmpty = null;
                        for (Hanging hanging : hangings) {
                            if (hanging.getFacing().ordinal() == getEntityData()) {
                                ItemFrame itemFrame = (ItemFrame) hanging;
                                if (itemFrame.getItem().getType() == Material.AIR) {
                                    firstEmpty = itemFrame;
                                } else if (isItemSimilar(itemFrame.getItem(), item)) {
                                    return BlockEditorResult.NO_ACTION;
                                }
                            }
                        }
                        if (firstEmpty != null) {
                            item.setAmount(-item.getAmount());
                            firstEmpty.setItem(item);
                            return BlockEditorResult.SUCCESS;
                        }
                    } else {
                        for (Hanging hanging : hangings) {
                            ItemFrame itemFrame = (ItemFrame) hanging;
                            if (hanging.getFacing().ordinal() == getEntityData() && isItemSimilar(itemFrame.getItem(), item)) {
                                itemFrame.setItem(new ItemStack(Material.AIR));
                                return BlockEditorResult.SUCCESS;
                            }
                        }
                    }
                } else {
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