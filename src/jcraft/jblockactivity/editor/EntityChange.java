package jcraft.jblockactivity.editor;

import static jcraft.jblockactivity.utils.ActivityUtil.isEntitySpawnClear;
import static jcraft.jblockactivity.utils.ActivityUtil.isItemSimilar;
import static jcraft.jblockactivity.utils.ActivityUtil.isSameLocation;
import static org.bukkit.Bukkit.getOfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.actionlog.EntityActionLog;
import jcraft.jblockactivity.extradata.EntityExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.AgeableExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.CreeperExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.EndermanExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.HorseExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.IronGolemExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.MagmaCubeExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.OcelotExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.PaintingExtraData;
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
import org.bukkit.entity.Painting;
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
                            if (getExtraData() != null && getEntityId() == 9) { // Painting
                                PaintingExtraData extraData = (PaintingExtraData) getExtraData();
                                if (((Painting) hanging).getArt() == extraData.getArt()) {
                                    return BlockEditorResult.NO_HANGING_ACTION;
                                }
                            } else {
                                return BlockEditorResult.NO_HANGING_ACTION;
                            }
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
                        if (getExtraData() != null && !getExtraData().isNull() && hanging instanceof Painting) {
                            final PaintingExtraData extraData = (PaintingExtraData) getExtraData();
                            ((Painting) hanging).setArt(extraData.getArt(), true);
                        }
                        return BlockEditorResult.HANGING_SPAWNED;
                    } catch (IllegalArgumentException e) {
                        throw new BlockEditorException("Invalid hanging block to place " + MaterialNames.entityName(getEntityId()),
                                block.getLocation());
                    }
                } else {
                    for (Hanging hanging : hangings) {
                        if (hanging.getFacing().ordinal() == getEntityData()) {
                            if (getExtraData() != null && !getExtraData().isNull() && getEntityId() == 9) { // Painting
                                PaintingExtraData extraData = (PaintingExtraData) getExtraData();
                                if (((Painting) hanging).getArt() == extraData.getArt()) {
                                    hanging.remove();
                                    return BlockEditorResult.HANGING_REMOVED;
                                }
                            } else {
                                hanging.remove();
                                return BlockEditorResult.HANGING_REMOVED;
                            }
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

                        final ItemStack[] entityEq = data.getEquipmentContent(getWorld());
                        if (entityEq != null) {
                            final Float[] dropChances = data.getEquipmentDropChances();
                            if (entityEq[0] != null) lEntity.getEquipment().setItemInHand(entityEq[0]);
                            if (dropChances[0] != null) lEntity.getEquipment().setItemInHandDropChance(dropChances[0]);
                            if (entityEq[1] != null) lEntity.getEquipment().setHelmet(entityEq[1]);
                            if (dropChances[1] != null) lEntity.getEquipment().setHelmetDropChance(dropChances[1]);
                            if (entityEq[2] != null) lEntity.getEquipment().setChestplate(entityEq[2]);
                            if (dropChances[2] != null) lEntity.getEquipment().setChestplateDropChance(dropChances[2]);
                            if (entityEq[3] != null) lEntity.getEquipment().setLeggings(entityEq[3]);
                            if (dropChances[3] != null) lEntity.getEquipment().setLeggingsDropChance(dropChances[3]);
                            if (entityEq[4] != null) lEntity.getEquipment().setBoots(entityEq[4]);
                            if (dropChances[4] != null) lEntity.getEquipment().setBootsDropChance(dropChances[4]);
                        }
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

                    if (!getExtraData().isNull()) {
                        if (getEntityId() == 50) {
                            final CreeperExtraData data = (CreeperExtraData) getExtraData();
                            ((Creeper) entity).setPowered(data.isPowered());
                        } else if (getEntityId() == 51) {
                            final SkeletonExtraData data = (SkeletonExtraData) getExtraData();
                            ((Skeleton) entity).setSkeletonType(data.getType());
                        } else if (getEntityId() == 54) {
                            final ZombieExtraData data = (ZombieExtraData) getExtraData();
                            final Zombie zombie = (Zombie) entity;
                            if (data.isBaby() != null) zombie.setBaby(data.isBaby());
                            if (data.isVillager() != null) zombie.setVillager(data.isVillager());
                        } else if (getEntityId() == 55) {
                            final SlimeExtraData data = (SlimeExtraData) getExtraData();
                            ((Slime) entity).setSize(data.getSize());
                        } else if (getEntityId() == 58) {
                            final EndermanExtraData data = (EndermanExtraData) getExtraData();
                            ((Enderman) entity).setCarriedMaterial(data.getMaterialData());
                        } else if (getEntityId() == 62) {
                            final MagmaCubeExtraData data = (MagmaCubeExtraData) getExtraData();
                            ((MagmaCube) entity).setSize(data.getSize());
                        } else if (getEntityId() == 90) {
                            final PigExtraData data = (PigExtraData) getExtraData();
                            ((Pig) entity).setSaddle(data.hasSaddle());
                        } else if (getEntityId() == 91) {
                            final SheepExtraData data = (SheepExtraData) getExtraData();
                            ((Sheep) entity).setColor(data.getColor());
                        } else if (getEntityId() == 95) {
                            final WolfExtraData data = (WolfExtraData) getExtraData();
                            final Wolf wolf = (Wolf) entity;
                            if (data.isTamed() != null) wolf.setTamed(data.isTamed());
                            if (data.getOwner() != null) {
                                final UUID ownerUUID = data.getOwnerUUID();
                                wolf.setOwner((ownerUUID == null) ? getOfflinePlayer(data.getOwner()) : getOfflinePlayer(ownerUUID));
                            }
                            if (data.getCollarColor() != null) wolf.setCollarColor(data.getCollarColor());
                        } else if (getEntityId() == 98) {
                            final OcelotExtraData data = (OcelotExtraData) getExtraData();
                            final Ocelot ocelot = (Ocelot) entity;
                            if (data.isTamed() != null) ocelot.setTamed(data.isTamed());
                            if (data.getOwner() != null) {
                                final UUID ownerUUID = data.getOwnerUUID();
                                ocelot.setOwner((ownerUUID == null) ? getOfflinePlayer(data.getOwner()) : getOfflinePlayer(ownerUUID));
                            }
                            if (data.getCatType() != null) ocelot.setCatType(data.getCatType());
                        } else if (getEntityId() == 99) {
                            final IronGolemExtraData data = (IronGolemExtraData) getExtraData();
                            ((IronGolem) entity).setPlayerCreated(data.isPlayerCreated());
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

                            final ItemStack[] invContent = data.getInventory(getWorld());
                            if (invContent != null) {
                                if (invContent[0] != null) horse.getInventory().setSaddle(invContent[0]);
                                if (invContent[1] != null) horse.getInventory().setArmor(invContent[1]);
                                if (horse.isCarryingChest() && invContent.length > 2) {
                                    for (int i = 2; i < horse.getInventory().getSize() + 2; i++) {
                                        if (invContent[i] != null) {
                                            horse.getInventory().setItem(i - 2, invContent[i]);
                                        }
                                    }
                                }
                            }

                            if (data.isTamed() != null) horse.setTamed(data.isTamed());
                            if (data.getOwner() != null) {
                                final UUID ownerUUID = data.getOwnerUUID();
                                horse.setOwner((ownerUUID == null) ? getOfflinePlayer(data.getOwner()) : getOfflinePlayer(ownerUUID));
                            }
                        } else if (getEntityId() == 120) {
                            final VillagerExtraData data = (VillagerExtraData) getExtraData();
                            ((Villager) entity).setProfession(data.getProfession());
                        }
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