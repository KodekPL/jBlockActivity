package jcraft.jblockactivity.extradata;

import org.bukkit.SkullType;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.material.MaterialData;

public abstract class BlockExtraData implements ExtraData {

    public abstract String getData();

    public static class CommandBlockExtraData extends BlockExtraData {
        private final String cmd, name;

        public CommandBlockExtraData(String name, String cmd) {
            this.name = name;
            this.cmd = cmd;
        }

        public CommandBlockExtraData(String data) {
            final String[] splitter = data.split("\0");
            this.name = splitter[0];
            this.cmd = splitter[1];
        }

        public String getName() {
            return this.name;
        }

        public String getCommand() {
            return this.cmd;
        }

        @Override
        public String getData() {
            return name + "\0" + cmd;
        }
    }

    public static class FlowerPotExtraData extends BlockExtraData {
        private int id;
        private byte data;

        public FlowerPotExtraData(int id, byte data) {
            this.id = id;
            this.data = data;
        }

        public FlowerPotExtraData(String data) {
            final String[] splitter = data.split("\0");
            try {
                this.id = Integer.parseInt(splitter[0]);
                this.data = Byte.parseByte(splitter[1]);
            } catch (Exception e) {
                this.id = 0;
                this.data = 0;
            }
        }

        public int getId() {
            return this.id;
        }

        public byte getItemData() {
            return this.data;
        }

        public MaterialData getMaterialData() {
            return new MaterialData(id, data);
        }

        @Override
        public String getData() {
            return id + "\0" + data;
        }
    }

    public static class MobSpawnerExtraData extends BlockExtraData {
        // Brane pod uwagę są tylko naturalnie wygenerowane Mob Spawnery
        private EntityType entityType;

        public MobSpawnerExtraData(EntityType entityType) {
            this.entityType = entityType;
        }

        public MobSpawnerExtraData(String data) {
            try {
                this.entityType = EntityType.valueOf(data.toUpperCase());
            } catch (Exception e) {
                this.entityType = null;
            }
        }

        public EntityType getEntityType() {
            return this.entityType;
        }

        @Override
        public String getData() {
            return entityType.name().toLowerCase();
        }
    }

    public static class SignExtraData extends BlockExtraData {
        private final String[] text;

        public SignExtraData(String[] text) {
            this.text = text;
        }

        public SignExtraData(String data) {
            this.text = data.split("\0", 4);
        }

        public String[] getText() {
            return this.text;
        }

        @Override
        public String getData() {
            if (text == null || text.length != 4) {
                return null;
            }
            return text[0] + "\0" + text[1] + "\0" + text[2] + "\0" + text[3];
        }
    }

    public static class SkullExtraData extends BlockExtraData {
        private BlockFace rotation;
        private String name;
        private SkullType skullType;

        public SkullExtraData(BlockFace rotation, String name, SkullType skullType) {
            this.rotation = rotation;
            this.name = name;
            this.skullType = skullType;
        }

        public SkullExtraData(String data) {
            final String[] splitter = data.split("\0");
            try {
                this.rotation = BlockFace.values()[Integer.parseInt(splitter[0])];
                this.name = splitter[1];
                this.skullType = SkullType.values()[Integer.parseInt(splitter[2])];
            } catch (Exception e) {
                this.rotation = BlockFace.NORTH;
                this.skullType = SkullType.SKELETON;
            }
        }

        public BlockFace getRotation() {
            return this.rotation;
        }

        public String getName() {
            return this.name;
        }

        public SkullType getSkullType() {
            return this.skullType;
        }

        @Override
        public String getData() {
            return rotation.ordinal() + "\0" + ((name == null) ? "null" : name) + "\0" + skullType.ordinal();
        }

    }

}
