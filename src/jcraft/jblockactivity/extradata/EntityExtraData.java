package jcraft.jblockactivity.extradata;

import static jcraft.jblockactivity.utils.ActivityUtil.toJson;

import org.bukkit.DyeColor;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.material.MaterialData;

public abstract class EntityExtraData implements ExtraData {

    /*
     * TODO: Horse inventory, TODO: Entities equipment
     */

    private String customName;
    private Boolean customNameVisible;
    private Boolean canPickupItems;
    private Boolean removeWhenFarAway;

    protected EntityExtraData(Entity entity) {
        if (entity instanceof LivingEntity) {
            final LivingEntity lEntity = (LivingEntity) entity;
            this.customName = lEntity.getCustomName();
            this.customNameVisible = lEntity.isCustomNameVisible() ? true : null;
            this.canPickupItems = lEntity.getCanPickupItems() ? true : null;
            this.removeWhenFarAway = lEntity.getRemoveWhenFarAway() ? null : false;
        }
    }

    public abstract String getData();

    public String getCustomName() {
        return customName;
    }

    public Boolean isCustomNameVisible() {
        return customNameVisible;
    }

    public Boolean getCanPickupItems() {
        return canPickupItems;
    }

    public Boolean getRemoveWhenFarAway() {
        return removeWhenFarAway;
    }

    public static class UnknownExtraData extends EntityExtraData {
        public UnknownExtraData(Entity entity) {
            super(entity);
        }

        @Override
        public String getData() {
            return toJson(this);
        }
    }

    public static class SkeletonExtraData extends EntityExtraData {
        private final SkeletonType type;

        public SkeletonExtraData(Skeleton entity) {
            super(entity);
            type = (entity.getSkeletonType() == SkeletonType.NORMAL) ? null : entity.getSkeletonType();
        }

        public SkeletonType getType() {
            return type;
        }

        @Override
        public String getData() {
            return toJson(this);
        }
    }

    public static class ZombieExtraData extends EntityExtraData {
        private final Boolean isBaby;
        private final Boolean isVillager;

        public ZombieExtraData(Zombie entity) {
            super(entity);
            isBaby = entity.isBaby() ? true : null;
            isVillager = entity.isVillager() ? true : null;
        }

        public Boolean isBaby() {
            return isBaby;
        }

        public Boolean isVillager() {
            return isVillager;
        }

        @Override
        public String getData() {
            return toJson(this);
        }
    }

    public static class MagmaCubeExtraData extends EntityExtraData {
        private final Integer size;

        public MagmaCubeExtraData(MagmaCube entity) {
            super(entity);
            size = entity.getSize();
        }

        public Integer getSize() {
            return size;
        }

        @Override
        public String getData() {
            return toJson(this);
        }
    }

    public static class SlimeExtraData extends EntityExtraData {
        private final Integer size;

        public SlimeExtraData(Slime entity) {
            super(entity);
            size = entity.getSize();
        }

        public Integer getSize() {
            return size;
        }

        @Override
        public String getData() {
            return toJson(this);
        }
    }

    public static class CreeperExtraData extends EntityExtraData {
        private final Boolean isPowered;

        public CreeperExtraData(Creeper entity) {
            super(entity);
            isPowered = entity.isPowered() ? true : null;
        }

        public Boolean isPowered() {
            return isPowered;
        }

        @Override
        public String getData() {
            return toJson(this);
        }
    }

    public static class EndermanExtraData extends EntityExtraData {
        private final Integer blockId;
        private final Byte blockData;

        public EndermanExtraData(Enderman entity) {
            super(entity);
            final MaterialData material = entity.getCarriedMaterial();
            if (material.getItemTypeId() == 0) {
                blockId = null;
                blockData = null;
            } else {
                blockId = material.getItemTypeId();
                blockData = (material.getData() == 0) ? null : material.getData();
            }
        }

        public Integer getBlockId() {
            return blockId;
        }

        public Byte getBlockData() {
            return blockData;
        }

        public MaterialData getMaterialData() {
            if (blockId == null || blockData == null) {
                return null;
            }
            return new MaterialData(blockId, blockData);
        }

        @Override
        public String getData() {
            return toJson(this);
        }
    }

    public static class IronGolemExtraData extends EntityExtraData {
        private final Boolean isPlayerCreated;

        public IronGolemExtraData(IronGolem entity) {
            super(entity);
            isPlayerCreated = entity.isPlayerCreated();
        }

        public Boolean isPlayerCreated() {
            return isPlayerCreated;
        }

        @Override
        public String getData() {
            return toJson(this);
        }
    }

    public static class AgeableExtraData extends EntityExtraData {
        private final Integer age;
        private final Boolean ageLock;
        private final Boolean isAdult;

        public AgeableExtraData(Ageable entity) {
            super(entity);
            this.age = (entity.getAge() == 0) ? null : entity.getAge();
            this.ageLock = entity.getAgeLock() ? true : null;
            this.isAdult = entity.isAdult() ? null : false;
        }

        public Integer getAge() {
            return age;
        }

        public Boolean isAgeLock() {
            return ageLock;
        }

        public Boolean isAdult() {
            return isAdult;
        }

        @Override
        public String getData() {
            return toJson(this);
        }
    }

    public static class VillagerExtraData extends AgeableExtraData {
        private final Profession profession;

        public VillagerExtraData(Villager entity) {
            super(entity);
            this.profession = (entity.getProfession() == Profession.FARMER) ? null : entity.getProfession();
        }

        public Profession getProfession() {
            return profession;
        }

        @Override
        public String getData() {
            return toJson(this);
        }
    }

    public static class HorseExtraData extends AgeableExtraData {
        private final Boolean isTamed;
        private final String owner;
        private final org.bukkit.entity.Horse.Variant variant;
        private final org.bukkit.entity.Horse.Color color;
        private final org.bukkit.entity.Horse.Style style;
        private final Integer maxDomestication;
        private final Integer domestication;
        private final Double jumpStrength;
        private final Boolean isCarryingChest;

        public HorseExtraData(Horse entity) {
            super(entity);
            isTamed = entity.isTamed() ? true : null;
            owner = (entity.getOwner() != null) ? entity.getOwner().getName() : null;
            variant = entity.getVariant();
            color = entity.getColor();
            style = entity.getStyle();
            maxDomestication = entity.getMaxDomestication();
            domestication = entity.getDomestication();
            jumpStrength = entity.getJumpStrength();
            isCarryingChest = entity.isCarryingChest() ? true : null;
        }

        public org.bukkit.entity.Horse.Variant getVariant() {
            return variant;
        }

        public org.bukkit.entity.Horse.Color getColor() {
            return color;
        }

        public org.bukkit.entity.Horse.Style getStyle() {
            return style;
        }

        public Integer getMaxDomestication() {
            return maxDomestication;
        }

        public Integer getDomestication() {
            return domestication;
        }

        public Double getJumpStrength() {
            return jumpStrength;
        }

        public Boolean isTamed() {
            return isTamed;
        }

        public String getOwner() {
            return owner;
        }

        public Boolean isCarryingChest() {
            return isCarryingChest;
        }

        @Override
        public String getData() {
            return toJson(this);
        }
    }

    public static class WolfExtraData extends AgeableExtraData {
        private final Boolean isTamed;
        private final String owner;
        private final Boolean isSitting;
        private final DyeColor collarColor;

        public WolfExtraData(Wolf entity) {
            super(entity);
            isTamed = entity.isTamed() ? true : null;
            owner = (entity.getOwner() != null) ? entity.getOwner().getName() : null;
            isSitting = entity.isSitting() ? true : null;
            collarColor = (entity.getCollarColor() == DyeColor.RED) ? null : entity.getCollarColor();
        }

        public Boolean isTamed() {
            return isTamed;
        }

        public String getOwner() {
            return owner;
        }

        public Boolean isSitting() {
            return isSitting;
        }

        public DyeColor getCollarColor() {
            return collarColor;
        }

        @Override
        public String getData() {
            return toJson(this);
        }
    }

    public static class OcelotExtraData extends AgeableExtraData {
        private final Boolean isTamed;
        private final String owner;
        private final Boolean isSitting;
        private final org.bukkit.entity.Ocelot.Type catType;

        public OcelotExtraData(Ocelot entity) {
            super(entity);
            isTamed = entity.isTamed() ? true : null;
            owner = (entity.getOwner() != null) ? entity.getOwner().getName() : null;
            isSitting = entity.isSitting() ? true : null;
            catType = (entity.getCatType() == org.bukkit.entity.Ocelot.Type.WILD_OCELOT) ? null : entity.getCatType();
        }

        public Boolean isTamed() {
            return isTamed;
        }

        public String getOwner() {
            return owner;
        }

        public Boolean isSitting() {
            return isSitting;
        }

        public org.bukkit.entity.Ocelot.Type getCatType() {
            return catType;
        }

        @Override
        public String getData() {
            return toJson(this);
        }
    }

    public static class PigExtraData extends AgeableExtraData {
        private final Boolean saddle;

        public PigExtraData(Pig entity) {
            super(entity);
            this.saddle = entity.hasSaddle() ? true : null;
        }

        public Boolean hasSaddle() {
            return saddle;
        }

        @Override
        public String getData() {
            return toJson(this);
        }
    }

    public static class SheepExtraData extends AgeableExtraData {
        private final DyeColor color;
        private final Boolean sheared;

        public SheepExtraData(Sheep entity) {
            super(entity);
            this.color = (entity.getColor() == DyeColor.WHITE) ? null : entity.getColor();
            this.sheared = entity.isSheared() ? true : null;
        }

        public DyeColor getColor() {
            return color;
        }

        public Boolean isSheared() {
            return sheared;
        }

        @Override
        public String getData() {
            return toJson(this);
        }
    }

    public static EntityExtraData getExtraData(Entity entity) {
        switch (entity.getType()) {
        case SHEEP:
            return new SheepExtraData((Sheep) entity);
        case PIG:
            return new PigExtraData((Pig) entity);
        case OCELOT:
            return new OcelotExtraData((Ocelot) entity);
        case WOLF:
            return new WolfExtraData((Wolf) entity);
        case HORSE:
            return new HorseExtraData((Horse) entity);
        case VILLAGER:
            return new VillagerExtraData((Villager) entity);
        case IRON_GOLEM:
            return new IronGolemExtraData((IronGolem) entity);
        case CREEPER:
            return new CreeperExtraData((Creeper) entity);
        case SLIME:
            return new SlimeExtraData((Slime) entity);
        case MAGMA_CUBE:
            return new MagmaCubeExtraData((MagmaCube) entity);
        case ZOMBIE:
            return new ZombieExtraData((Zombie) entity);
        case SKELETON:
            return new SkeletonExtraData((Skeleton) entity);
        case ENDERMAN:
            return new EndermanExtraData((Enderman) entity);
        default:
            return null;
        }
    }

}
