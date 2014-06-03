package jcraft.jblockactivity.extradata;

import static jcraft.jblockactivity.utils.ActivityUtil.toJson;

import org.bukkit.SkullType;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Skull;
import org.bukkit.entity.EntityType;
import org.bukkit.material.MaterialData;

public abstract class BlockExtraData implements ExtraData {

    public abstract String getData();

    public abstract boolean isNull();

    public static class CommandBlockExtraData extends BlockExtraData {

        private final String cmd, name;

        public CommandBlockExtraData(String name, String cmd) {
            this.name = name.equals("@") ? null : name;
            this.cmd = cmd;
        }

        public String getName() {
            return name;
        }

        public String getCommand() {
            return cmd;
        }

        @Override
        public String getData() {
            return toJson(this);
        }

        @Override
        public boolean isNull() {
            return name == null && cmd == null;
        }

    }

    @Deprecated
    public static class FlowerPotExtraData extends BlockExtraData { // https://bukkit.atlassian.net/browse/BUKKIT-5316

        private final Integer itemId;
        private final Byte itemData;

        public FlowerPotExtraData(int id, byte data) {
            this.itemId = (id == 0) ? null : id;
            this.itemData = (data == 0) ? null : data;
        }

        public Integer getItemId() {
            return itemId;
        }

        public Byte getItemData() {
            return itemData;
        }

        public MaterialData getMaterialData() {
            return new MaterialData(itemId, itemData);
        }

        @Override
        public String getData() {
            return toJson(this);
        }

        @Override
        public boolean isNull() {
            return itemId == null || itemData == null;
        }

    }

    public static class MobSpawnerExtraData extends BlockExtraData { // Only naturally generated Mob Spawners

        private final EntityType entityType;

        public MobSpawnerExtraData(EntityType entityType) {
            this.entityType = entityType;
        }

        public EntityType getEntityType() {
            return entityType;
        }

        @Override
        public String getData() {
            return toJson(this);
        }

        @Override
        public boolean isNull() {
            return entityType == null;
        }

    }

    public static class SignExtraData extends BlockExtraData {

        private String[] text;

        public SignExtraData(String[] text) {
            if (text.length == 4 && !text[0].equals("") && !text[1].equals("") && !text[2].equals("") && !text[3].equals("")) {
                this.text = text;
            }
        }

        public String[] getText() {
            return text;
        }

        @Override
        public String getData() {
            return toJson(this);
        }

        @Override
        public boolean isNull() {
            return text == null;
        }

    }

    public static class SkullExtraData extends BlockExtraData {

        private final BlockFace rotation;
        private final String name;
        private final SkullType skullType;

        public SkullExtraData(BlockFace rotation, String name, SkullType skullType) {
            this.rotation = rotation;
            this.name = name;
            this.skullType = (skullType == SkullType.SKELETON) ? null : skullType;
        }

        public BlockFace getRotation() {
            return rotation;
        }

        public String getName() {
            return name;
        }

        public SkullType getSkullType() {
            return skullType;
        }

        @Override
        public String getData() {
            return toJson(this);
        }

        @Override
        public boolean isNull() {
            return skullType == null || rotation == null;
        }

    }

    public static BlockExtraData getExtraData(BlockState state) {
        switch (state.getType()) {
        case WALL_SIGN:
        case SIGN_POST:
            return new SignExtraData(((org.bukkit.block.Sign) state).getLines());
        case SKULL:
            final Skull skull = (Skull) state;
            return new SkullExtraData(skull.getRotation(), skull.getOwner(), skull.getSkullType());
        case MOB_SPAWNER:
            return new MobSpawnerExtraData(((CreatureSpawner) state).getSpawnedType());
        case COMMAND:
            final CommandBlock cmd = (CommandBlock) state;
            return new CommandBlockExtraData(cmd.getName(), cmd.getCommand());
        default:
            return null;
        }
    }

}
