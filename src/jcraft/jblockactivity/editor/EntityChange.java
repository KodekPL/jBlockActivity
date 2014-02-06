package jcraft.jblockactivity.editor;

import static jcraft.jblockactivity.utils.ActivityUtil.isEntitySpawnClear;
import static jcraft.jblockactivity.utils.ActivityUtil.isItemSimilar;
import static jcraft.jblockactivity.utils.ActivityUtil.isSameLocation;
import static org.bukkit.Bukkit.getOfflinePlayer;

import java.util.ArrayList;
import java.util.List;

import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.actionlogs.EntityActionLog;
import jcraft.jblockactivity.extradata.EntityExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.AgeableExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.CreeperExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.EndermanExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.HorseExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.IronGolemExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.MagmaCubeExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.OcelotExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.PigExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.SheepExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.SkeletonExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.SlimeExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.VillagerExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.WolfExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.ZombieExtraData;
import jcraft.jblockactivity.extradata.ExtraData;
import jcraft.jblockactivity.extradata.InventoryExtraData;
import jcraft.jblockactivity.utils.MaterialNames;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Horse;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class EntityChange extends EntityActionLog {

    public EntityChange(LoggingType type, String playerName, World world, Vector location, int entityId, int dataId, ExtraData extraData) {
        super(type, playerName, world, location, entityId, dataId, extraData);
    }

    public BlockEditorResult perform(BlockEditor blockEditor) throws BlockEditorException {
        if (getLogInstance() instanceof EntityActionLog) {
            final Block block = getLocation().getBlock();
            final Chunk chunk = block.getChunk();
            if (getLoggingType() == LoggingType.hangingbreak || getLoggingType() == LoggingType.hangingplace) {
                final Hanging[] hangings = getHangings(chunk, getLocation().toVector(), getEntityId());
                final BlockFace face = BlockFace.values()[getEntityData()];
                final Block hangBlock = block.getRelative(face.getOppositeFace());

                if (getLoggingType() == LoggingType.hangingbreak && !blockEditor.isRedo()) {
                    for (Hanging hanging : hangings) {
                        if (hanging.getFacing().ordinal() == getEntityData()) {
                            return BlockEditorResult.NO_HANGING_ACTION;
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
                        return BlockEditorResult.HANGING_SPAWNED;
                    } catch (IllegalArgumentException e) {
                        throw new BlockEditorException("Invalid hanging block to place " + MaterialNames.entityName(getEntityId()),
                                block.getLocation());
                    }
                } else {
                    for (Hanging hanging : hangings) {
                        if (hanging.getFacing().ordinal() == getEntityData()) {
                            hanging.remove();
                            return BlockEditorResult.HANGING_REMOVED;
                        }
                    }
                    return BlockEditorResult.NO_HANGING_ACTION;
                }
            } else if (getLoggingType() == LoggingType.hanginginteract) {
                // Only for item frames for now (if Mojang will add some kind of new item frame)
                if (getExtraData() != null) {
                    final Hanging[] hangings = getHangings(chunk, getLocation().toVector(), getEntityId());
                    final InventoryExtraData extraData = (InventoryExtraData) getExtraData();
                    if (extraData.getContent().length == 0) {
                        return BlockEditorResult.NO_INVENTORY_ACTION;
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
                                    return BlockEditorResult.NO_INVENTORY_ACTION;
                                }
                            }
                        }
                        if (firstEmpty != null) {
                            item.setAmount(-item.getAmount());
                            firstEmpty.setItem(item);
                            return BlockEditorResult.INVENTORY_ACCESS;
                        }
                    } else {
                        for (Hanging hanging : hangings) {
                            ItemFrame itemFrame = (ItemFrame) hanging;
                            if (hanging.getFacing().ordinal() == getEntityData() && isItemSimilar(itemFrame.getItem(), item)) {
                                itemFrame.setItem(new ItemStack(Material.AIR));
                                return BlockEditorResult.INVENTORY_ACCESS;
                            }
                        }
                    }
                } else {
                    return BlockEditorResult.NO_INVENTORY_ACTION;
                }
            } else if (getLoggingType() == LoggingType.creaturekill) {
                if (!isEntitySpawnClear(block.getType())) {
                    return BlockEditorResult.NO_ENTITY_ACTION;
                }
                final EntityType type = EntityType.fromId(getEntityId());
                if (blockEditor.getWorldConfig().limitEntitiesPerChunk > 0) {
                    final int count = countEntitiesTypes(chunk.getEntities(), type);
                    if (count > blockEditor.getWorldConfig().limitEntitiesPerChunk) {
                        return BlockEditorResult.NO_ENTITY_ACTION;
                    }
                }
                final Entity entity = getWorld().spawn(getLocation(), type.getEntityClass());
                if (getExtraData() != null) {

                    if (entity instanceof LivingEntity) {
                        final EntityExtraData data = (EntityExtraData) getExtraData();
                        final LivingEntity lEntity = (LivingEntity) entity;
                        if (data.getCustomName() != null) lEntity.setCustomName(data.getCustomName());
                        if (data.isCustomNameVisible() != null) lEntity.setCustomNameVisible(data.isCustomNameVisible());
                        if (data.getCanPickupItems() != null) lEntity.setCanPickupItems(data.getCanPickupItems());
                        if (data.getRemoveWhenFarAway() != null) lEntity.setRemoveWhenFarAway(data.getRemoveWhenFarAway());
                    }

                    if (entity instanceof Ageable) {
                        final AgeableExtraData data = (AgeableExtraData) getExtraData();
                        final Ageable ageable = (Ageable) entity;
                        if (data.getAge() != null) ageable.setAge(data.getAge());
                        if (data.isAgeLock() != null) ageable.setAgeLock(data.isAgeLock());
                        if (data.isAdult() != null) {
                            if (data.isAdult()) {
                                ageable.setAdult();
                            } else {
                                ageable.setBaby();
                            }
                        }
                    }

                    if (getEntityId() == 50) {
                        final CreeperExtraData data = (CreeperExtraData) getExtraData();
                        if (data.isPowered() != null) ((Creeper) entity).setPowered(data.isPowered());
                    } else if (getEntityId() == 51) {
                        final SkeletonExtraData data = (SkeletonExtraData) getExtraData();
                        if (data.getType() != null) ((Skeleton) entity).setSkeletonType(data.getType());
                    } else if (getEntityId() == 54) {
                        final ZombieExtraData data = (ZombieExtraData) getExtraData();
                        final Zombie zombie = (Zombie) entity;
                        if (data.isBaby() != null) zombie.setBaby(data.isBaby());
                        if (data.isVillager() != null) zombie.setVillager(data.isVillager());
                    } else if (getEntityId() == 55) {
                        final SlimeExtraData data = (SlimeExtraData) getExtraData();
                        if (data.getSize() != null) ((Slime) entity).setSize(data.getSize());
                    } else if (getEntityId() == 58) {
                        final EndermanExtraData data = (EndermanExtraData) getExtraData();
                        if (data.getMaterialData() != null) ((Enderman) entity).setCarriedMaterial(data.getMaterialData());
                    } else if (getEntityId() == 62) {
                        final MagmaCubeExtraData data = (MagmaCubeExtraData) getExtraData();
                        if (data.getSize() != null) ((MagmaCube) entity).setSize(data.getSize());
                    } else if (getEntityId() == 90) {
                        final PigExtraData data = (PigExtraData) getExtraData();
                        if (data.hasSaddle() != null) ((Pig) entity).setSaddle(data.hasSaddle());
                    } else if (getEntityId() == 91) {
                        final SheepExtraData data = (SheepExtraData) getExtraData();
                        if (data.getColor() != null) ((Sheep) entity).setColor(data.getColor());
                    } else if (getEntityId() == 95) {
                        final WolfExtraData data = (WolfExtraData) getExtraData();
                        final Wolf wolf = (Wolf) entity;
                        if (data.isTamed() != null) wolf.setTamed(data.isTamed());
                        if (data.getOwner() != null) {
                            wolf.setOwner(getOfflinePlayer(data.getOwner()));
                        }
                        if (data.getCollarColor() != null) wolf.setCollarColor(data.getCollarColor());
                    } else if (getEntityId() == 98) {
                        final OcelotExtraData data = (OcelotExtraData) getExtraData();
                        final Ocelot ocelot = (Ocelot) entity;
                        if (data.isTamed() != null) ocelot.setTamed(data.isTamed());
                        if (data.getOwner() != null) {
                            ocelot.setOwner(getOfflinePlayer(data.getOwner()));
                        }
                        if (data.getCatType() != null) ocelot.setCatType(data.getCatType());
                    } else if (getEntityId() == 99) {
                        final IronGolemExtraData data = (IronGolemExtraData) getExtraData();
                        if (data.isPlayerCreated() != null) ((IronGolem) entity).setPlayerCreated(data.isPlayerCreated());
                    } else if (getEntityId() == 100) {
                        final HorseExtraData data = (HorseExtraData) getExtraData();
                        final Horse horse = (Horse) entity;
                        if (data.getVariant() != null) horse.setVariant(data.getVariant());
                        if (data.getColor() != null) horse.setColor(data.getColor());
                        if (data.getStyle() != null) horse.setStyle(data.getStyle());
                        if (data.getMaxDomestication() != null) horse.setMaxDomestication(data.getMaxDomestication());
                        if (data.getDomestication() != null) horse.setDomestication(data.getDomestication());
                        if (data.getJumpStrength() != null) horse.setJumpStrength(data.getJumpStrength());
                        if (data.isCarryingChest() != null) horse.setCarryingChest(data.isCarryingChest());
                        if (data.isTamed() != null) horse.setTamed(data.isTamed());
                        if (data.getOwner() != null) {
                            horse.setOwner(getOfflinePlayer(data.getOwner()));
                        }
                    } else if (getEntityId() == 120) {
                        final VillagerExtraData data = (VillagerExtraData) getExtraData();
                        if (data.getProfession() != null) ((Villager) entity).setProfession(data.getProfession());
                    }
                }
                return BlockEditorResult.ENTITY_SPAWNED;
            }
            return BlockEditorResult.NO_ENTITY_ACTION;
        } else {
            return BlockEditorResult.NO_ENTITY_ACTION;
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

    private int countEntitiesTypes(Entity[] entities, EntityType type) {
        int count = 0;
        for (Entity entity : entities) {
            if (entity.getType() == type) {
                count++;
            }
        }
        return count;
    }

}