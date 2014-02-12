package jcraft.jblockactivity.extradata;

import static jcraft.jblockactivity.utils.ActivityUtil.packEntityEquipment;
import static jcraft.jblockactivity.utils.ActivityUtil.packEntityEquipmentChance;
import static jcraft.jblockactivity.utils.ActivityUtil.toJson;
import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.WorldConfig;
import jcraft.jblockactivity.extradata.ExtraLoggingTypes.EntityMetaType;
import jcraft.jblockactivity.extradata.InventoryExtraData.SimpleItemMeta;

import org.bukkit.Art;
import org.bukkit.DyeColor;
import org.bukkit.World;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public abstract class EntityExtraData implements ExtraData {
    private String customName;
    private Boolean customNameVisible;
    private Boolean canPickupItems;
    private Boolean removeWhenFarAway;

    private String[] equipmentContent;
    private SimpleItemMeta[] equipmentMeta;
    private Float[] dropChance;

    protected EntityExtraData(WorldConfig config, Entity entity) {
        if (entity instanceof LivingEntity) {
            final LivingEntity lEntity = (LivingEntity) entity;
            if (config.isLoggingExtraEntityMeta(EntityMetaType.customname)) this.customName = lEntity.getCustomName();
            if (config.isLoggingExtraEntityMeta(EntityMetaType.customname)) this.customNameVisible = lEntity.isCustomNameVisible() ? true : null;
            this.canPickupItems = lEntity.getCanPickupItems() ? true : null;
            this.removeWhenFarAway = lEntity.getRemoveWhenFarAway() ? null : false;

            if (config.isLoggingExtraEntityMeta(EntityMetaType.equipment)) {
                final ItemStack[] eqArray = packEntityEquipment(lEntity.getEquipment());
                if (eqArray != null) {
                    dropChance = packEntityEquipmentChance(lEntity.getEquipment());
                    final InventoryExtraData invExtraData = new InventoryExtraData(eqArray, false, config);
                    equipmentContent = invExtraData.getStringContent();
                    equipmentMeta = invExtraData.getSimpleItemMeta();
                }
            }
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

    public ItemStack[] getEquipmentContent(World world) {
        if (equipmentContent != null) {
            final InventoryExtraData invExtraData = new InventoryExtraData(equipmentContent, equipmentMeta, world);
            return invExtraData.getContent();
        }
        return null;
    }

    public Float[] getEquipmentDropChances() {
        return dropChance;
    }

    public static class UnknownExtraData extends EntityExtraData {
        public UnknownExtraData(WorldConfig config, Entity entity) {
            super(config, entity);
        }

        @Override
        public String getData() {
            return toJson(this);
        }
    }

    public static class SkeletonExtraData extends EntityExtraData {
        private SkeletonType type;

        public SkeletonExtraData(WorldConfig config, Skeleton entity) {
            super(config, entity);
            if (config.isLoggingExtraEntityMeta(EntityMetaType.skeletontype)) type = (entity.getSkeletonType() == SkeletonType.NORMAL) ? null
                    : entity.getSkeletonType();
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
        private Boolean isBaby;
        private Boolean isVillager;

        public ZombieExtraData(WorldConfig config, Zombie entity) {
            super(config, entity);
            if (config.isLoggingExtraEntityMeta(EntityMetaType.zombiebaby)) isBaby = entity.isBaby() ? true : null;
            if (config.isLoggingExtraEntityMeta(EntityMetaType.zombievillager)) isVillager = entity.isVillager() ? true : null;
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
        private Integer size;

        public MagmaCubeExtraData(WorldConfig config, MagmaCube entity) {
            super(config, entity);
            if (config.isLoggingExtraEntityMeta(EntityMetaType.magmacubesize)) size = entity.getSize();
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
        private Integer size;

        public SlimeExtraData(WorldConfig config, Slime entity) {
            super(config, entity);
            if (config.isLoggingExtraEntityMeta(EntityMetaType.slimesize)) size = entity.getSize();
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
        private Boolean isPowered;

        public CreeperExtraData(WorldConfig config, Creeper entity) {
            super(config, entity);
            if (config.isLoggingExtraEntityMeta(EntityMetaType.creeperpowered)) isPowered = entity.isPowered() ? true : null;
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
        private Integer blockId;
        private Byte blockData;

        public EndermanExtraData(WorldConfig config, Enderman entity) {
            super(config, entity);
            if (config.isLoggingExtraEntityMeta(EntityMetaType.endermancarriedblock)) {
                final MaterialData material = entity.getCarriedMaterial();
                if (material.getItemTypeId() == 0) {
                    blockId = null;
                    blockData = null;
                } else {
                    blockId = material.getItemTypeId();
                    blockData = (material.getData() == 0) ? null : material.getData();
                }
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
        private Boolean isPlayerCreated;

        public IronGolemExtraData(WorldConfig config, IronGolem entity) {
            super(config, entity);
            if (config.isLoggingExtraEntityMeta(EntityMetaType.irongolemplayercreation)) isPlayerCreated = entity.isPlayerCreated();
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
        private Integer age;
        private Boolean ageLock;
        private Boolean isAdult;

        public AgeableExtraData(WorldConfig config, Ageable entity) {
            super(config, entity);
            if (config.isLoggingExtraEntityMeta(EntityMetaType.age)) this.age = (entity.getAge() == 0) ? null : entity.getAge();
            if (config.isLoggingExtraEntityMeta(EntityMetaType.age)) this.ageLock = entity.getAgeLock() ? true : null;
            if (config.isLoggingExtraEntityMeta(EntityMetaType.animalbaby)) this.isAdult = entity.isAdult() ? null : false;
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
        private Profession profession;

        public VillagerExtraData(WorldConfig config, Villager entity) {
            super(config, entity);
            if (config.isLoggingExtraEntityMeta(EntityMetaType.villagerproffesion)) this.profession = (entity.getProfession() == Profession.FARMER) ? null
                    : entity.getProfession();
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
        private Boolean isTamed;
        private String owner;
        private org.bukkit.entity.Horse.Variant variant;
        private org.bukkit.entity.Horse.Color color;
        private org.bukkit.entity.Horse.Style style;
        private Integer maxDomestication;
        private Integer domestication;
        private Double jumpStrength;
        private Boolean isCarryingChest;
        private String[] inventoryContent;
        private SimpleItemMeta[] inventoryMeta;

        public HorseExtraData(WorldConfig config, Horse entity) {
            super(config, entity);
            if (config.isLoggingExtraEntityMeta(EntityMetaType.horseowner)) isTamed = entity.isTamed() ? true : null;
            if (config.isLoggingExtraEntityMeta(EntityMetaType.horseowner)) owner = (entity.getOwner() != null) ? entity.getOwner().getName() : null;
            if (config.isLoggingExtraEntityMeta(EntityMetaType.horselook)) variant = entity.getVariant();
            if (config.isLoggingExtraEntityMeta(EntityMetaType.horselook)) color = entity.getColor();
            if (config.isLoggingExtraEntityMeta(EntityMetaType.horselook)) style = entity.getStyle();
            maxDomestication = entity.getMaxDomestication();
            domestication = entity.getDomestication();
            jumpStrength = entity.getJumpStrength();
            if (config.isLoggingExtraEntityMeta(EntityMetaType.horseinventory)) {
                isCarryingChest = entity.isCarryingChest() ? true : null;

                if (entity.isCarryingChest()) {
                    inventoryContent = new String[entity.getInventory().getSize() + 2];
                    inventoryMeta = new SimpleItemMeta[entity.getInventory().getSize() + 2];
                } else {
                    inventoryContent = new String[2];
                    inventoryMeta = new SimpleItemMeta[2];
                }

                final ItemStack[] content = new ItemStack[inventoryContent.length];
                content[0] = entity.getInventory().getSaddle();
                content[1] = entity.getInventory().getArmor();
                if (content.length > 2) {
                    for (int i = 2; i < entity.getInventory().getSize() + 2; i++) {
                        content[i] = entity.getInventory().getItem(i - 2);
                    }
                }

                final InventoryExtraData invExtraData = new InventoryExtraData(content, false, config);
                inventoryContent = invExtraData.getStringContent();
                inventoryMeta = invExtraData.getSimpleItemMeta();
            }
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

        public ItemStack[] getInventory(World world) {
            if (inventoryContent != null) {
                final InventoryExtraData invExtraData = new InventoryExtraData(inventoryContent, inventoryMeta, world);
                return invExtraData.getContent();
            }
            return null;
        }

        @Override
        public String getData() {
            return toJson(this);
        }
    }

    public static class WolfExtraData extends AgeableExtraData {
        private Boolean isTamed;
        private String owner;
        private Boolean isSitting;
        private DyeColor collarColor;

        public WolfExtraData(WorldConfig config, Wolf entity) {
            super(config, entity);
            if (config.isLoggingExtraEntityMeta(EntityMetaType.wolfowner)) isTamed = entity.isTamed() ? true : null;
            if (config.isLoggingExtraEntityMeta(EntityMetaType.wolfowner)) owner = (entity.getOwner() != null) ? entity.getOwner().getName() : null;
            if (config.isLoggingExtraEntityMeta(EntityMetaType.wolfowner)) isSitting = entity.isSitting() ? true : null;
            if (config.isLoggingExtraEntityMeta(EntityMetaType.wolfcollar)) collarColor = (entity.getCollarColor() == DyeColor.RED) ? null : entity
                    .getCollarColor();
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
        private Boolean isTamed;
        private String owner;
        private Boolean isSitting;
        private org.bukkit.entity.Ocelot.Type catType;

        public OcelotExtraData(WorldConfig config, Ocelot entity) {
            super(config, entity);
            if (config.isLoggingExtraEntityMeta(EntityMetaType.catowner)) isTamed = entity.isTamed() ? true : null;
            if (config.isLoggingExtraEntityMeta(EntityMetaType.catowner)) owner = (entity.getOwner() != null) ? entity.getOwner().getName() : null;
            if (config.isLoggingExtraEntityMeta(EntityMetaType.catowner)) isSitting = entity.isSitting() ? true : null;
            if (config.isLoggingExtraEntityMeta(EntityMetaType.cattype)) catType = (entity.getCatType() == org.bukkit.entity.Ocelot.Type.WILD_OCELOT) ? null
                    : entity.getCatType();
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
        private Boolean saddle;

        public PigExtraData(WorldConfig config, Pig entity) {
            super(config, entity);
            if (config.isLoggingExtraEntityMeta(EntityMetaType.pigsaddle)) saddle = entity.hasSaddle() ? true : null;
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
        private DyeColor color;
        private Boolean sheared;

        public SheepExtraData(WorldConfig config, Sheep entity) {
            super(config, entity);
            if (config.isLoggingExtraEntityMeta(EntityMetaType.sheepcolor)) color = (entity.getColor() == DyeColor.WHITE) ? null : entity.getColor();
            if (config.isLoggingExtraEntityMeta(EntityMetaType.sheepsheard)) sheared = entity.isSheared() ? true : null;
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

    public static class PaintingExtraData extends EntityExtraData {
        private final Art art;

        public PaintingExtraData(WorldConfig config, Painting entity) {
            super(config, entity);
            this.art = entity.getArt();
        }

        public Art getArt() {
            return art;
        }

        @Override
        public String getData() {
            return toJson(this);
        }
    }

    public static EntityExtraData getExtraData(Entity entity) {
        WorldConfig config = BlockActivity.getWorldConfig(entity.getWorld().getName());
        switch (entity.getType()) {
        case SHEEP:
            return new SheepExtraData(config, (Sheep) entity);
        case PIG:
            return new PigExtraData(config, (Pig) entity);
        case OCELOT:
            return new OcelotExtraData(config, (Ocelot) entity);
        case WOLF:
            return new WolfExtraData(config, (Wolf) entity);
        case HORSE:
            return new HorseExtraData(config, (Horse) entity);
        case VILLAGER:
            return new VillagerExtraData(config, (Villager) entity);
        case IRON_GOLEM:
            return new IronGolemExtraData(config, (IronGolem) entity);
        case CREEPER:
            return new CreeperExtraData(config, (Creeper) entity);
        case SLIME:
            return new SlimeExtraData(config, (Slime) entity);
        case MAGMA_CUBE:
            return new MagmaCubeExtraData(config, (MagmaCube) entity);
        case ZOMBIE:
            return new ZombieExtraData(config, (Zombie) entity);
        case SKELETON:
            return new SkeletonExtraData(config, (Skeleton) entity);
        case ENDERMAN:
            return new EndermanExtraData(config, (Enderman) entity);
        case PAINTING:
            return new PaintingExtraData(config, (Painting) entity);
        default:
            return null;
        }
    }
}
