package jcraft.jblockactivity.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jcraft.jblockactivity.BlockActivity;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

public class MaterialNames {

    private static Map<String, String> MATERIAL_NAMES = new LinkedHashMap<String, String>();

    public static void loadMaterialNames() {
        final File materialsFile = new File(BlockActivity.dataFolder, "materials.yml");

        if (!materialsFile.exists()) {
            MATERIAL_NAMES = new LinkedHashMap<String, String>();
            saveMaterialNames();
        }

        MATERIAL_NAMES = new HashMap<String, String>();

        final YamlConfiguration yml = YamlConfiguration.loadConfiguration(materialsFile);

        final Set<String> materials = yml.getConfigurationSection("materials").getKeys(false);

        for (String key : materials) {
            MATERIAL_NAMES.put(key, yml.getString("materials." + key));
        }
    }

    public static void saveMaterialNames() {
        final File materialsFile = new File(BlockActivity.dataFolder, "materials.yml");
        final YamlConfiguration yml = YamlConfiguration.loadConfiguration(materialsFile);

        addMaterialName(Material.WOOD, 1, "spruce wood planks");
        addMaterialName(Material.WOOD, 2, "birch wood planks");
        addMaterialName(Material.WOOD, 3, "jungle wood planks");
        addMaterialName(Material.WOOD, 4, "acacia wood planks");
        addMaterialName(Material.WOOD, 5, "dark oak wood planks");

        addMaterialName(Material.DIRT, 1, "coarse dirt");
        addMaterialName(Material.DIRT, 2, "podzol");

        addMaterialName(Material.SAPLING, 0, "oak sapling");
        addMaterialName(Material.SAPLING, 1, "spruce sapling");
        addMaterialName(Material.SAPLING, 2, "birch sapling");
        addMaterialName(Material.SAPLING, 3, "jungle sapling");
        addMaterialName(Material.SAPLING, 4, "acacia sapling");
        addMaterialName(Material.SAPLING, 5, "dark oak sapling");

        addMaterialName(Material.SAND, 1, "red sand");

        addMaterialName(Material.LOG, 0, "oak log");
        addMaterialName(Material.LOG, 4, "oak log");
        addMaterialName(Material.LOG, 8, "oak log");
        addMaterialName(Material.LOG, 12, "oak log");

        addMaterialName(Material.LOG, 1, "spruce log");
        addMaterialName(Material.LOG, 5, "spruce log");
        addMaterialName(Material.LOG, 9, "spruce log");
        addMaterialName(Material.LOG, 13, "spruce log");

        addMaterialName(Material.LOG, 2, "birch log");
        addMaterialName(Material.LOG, 6, "birch log");
        addMaterialName(Material.LOG, 10, "birch log");
        addMaterialName(Material.LOG, 14, "birch log");

        addMaterialName(Material.LOG, 3, "jungle log");
        addMaterialName(Material.LOG, 7, "jungle log");
        addMaterialName(Material.LOG, 11, "jungle log");
        addMaterialName(Material.LOG, 15, "jungle log");

        addMaterialName(Material.LOG_2, 0, "acacia log");
        addMaterialName(Material.LOG_2, 4, "acacia log");
        addMaterialName(Material.LOG_2, 8, "acacia log");
        addMaterialName(Material.LOG_2, 12, "acacia log");

        addMaterialName(Material.LOG_2, 1, "dark oak log");
        addMaterialName(Material.LOG_2, 5, "dark oak log");
        addMaterialName(Material.LOG_2, 9, "dark oak log");
        addMaterialName(Material.LOG_2, 13, "dark oak log");

        addMaterialName(Material.LEAVES, 0, "oak leaves");
        addMaterialName(Material.LEAVES, 1, "spruce leaves");
        addMaterialName(Material.LEAVES, 2, "birch leaves");
        addMaterialName(Material.LEAVES, 3, "jungle leaves");

        addMaterialName(Material.LEAVES_2, 0, "acacia leaves");
        addMaterialName(Material.LEAVES_2, 1, "dark oak leaves");

        addMaterialName(Material.WOOL, 0, "white wool");
        addMaterialName(Material.WOOL, 1, "orange wool");
        addMaterialName(Material.WOOL, 2, "magenta wool");
        addMaterialName(Material.WOOL, 3, "light blue wool");
        addMaterialName(Material.WOOL, 4, "yellow wool");
        addMaterialName(Material.WOOL, 5, "lime wool");
        addMaterialName(Material.WOOL, 6, "pink wool");
        addMaterialName(Material.WOOL, 7, "gray wool");
        addMaterialName(Material.WOOL, 8, "light gray wool");
        addMaterialName(Material.WOOL, 9, "cyan wool");
        addMaterialName(Material.WOOL, 10, "purple wool");
        addMaterialName(Material.WOOL, 11, "blue wool");
        addMaterialName(Material.WOOL, 12, "brown wool");
        addMaterialName(Material.WOOL, 13, "green wool");
        addMaterialName(Material.WOOL, 14, "red wool");
        addMaterialName(Material.WOOL, 15, "black wool");

        addMaterialName(Material.CARPET, 0, "white carpet");
        addMaterialName(Material.CARPET, 1, "orange carpet");
        addMaterialName(Material.CARPET, 2, "magenta carpet");
        addMaterialName(Material.CARPET, 3, "light blue carpet");
        addMaterialName(Material.CARPET, 4, "yellow carpet");
        addMaterialName(Material.CARPET, 5, "lime carpet");
        addMaterialName(Material.CARPET, 6, "pink carpet");
        addMaterialName(Material.CARPET, 7, "gray carpet");
        addMaterialName(Material.CARPET, 8, "light gray carpet");
        addMaterialName(Material.CARPET, 9, "cyan carpet");
        addMaterialName(Material.CARPET, 10, "purple carpet");
        addMaterialName(Material.CARPET, 11, "blue carpet");
        addMaterialName(Material.CARPET, 12, "brown carpet");
        addMaterialName(Material.CARPET, 13, "green carpet");
        addMaterialName(Material.CARPET, 14, "red carpet");
        addMaterialName(Material.CARPET, 15, "black carpet");

        addMaterialName(Material.STAINED_CLAY, 0, "white stained clay");
        addMaterialName(Material.STAINED_CLAY, 1, "orange stained clay");
        addMaterialName(Material.STAINED_CLAY, 2, "magenta stained clay");
        addMaterialName(Material.STAINED_CLAY, 3, "light blue stained clay");
        addMaterialName(Material.STAINED_CLAY, 4, "yellow stained clay");
        addMaterialName(Material.STAINED_CLAY, 5, "lime stained clay");
        addMaterialName(Material.STAINED_CLAY, 6, "pink stained clay");
        addMaterialName(Material.STAINED_CLAY, 7, "gray stained clay");
        addMaterialName(Material.STAINED_CLAY, 8, "light gray stained clay");
        addMaterialName(Material.STAINED_CLAY, 9, "cyan stained clay");
        addMaterialName(Material.STAINED_CLAY, 10, "purple stained clay");
        addMaterialName(Material.STAINED_CLAY, 11, "blue stained clay");
        addMaterialName(Material.STAINED_CLAY, 12, "brown stained clay");
        addMaterialName(Material.STAINED_CLAY, 13, "green stained clay");
        addMaterialName(Material.STAINED_CLAY, 14, "red stained clay");
        addMaterialName(Material.STAINED_CLAY, 15, "black stained clay");

        addMaterialName(Material.STAINED_GLASS, 0, "white stained glass");
        addMaterialName(Material.STAINED_GLASS, 1, "orange stained glass");
        addMaterialName(Material.STAINED_GLASS, 2, "magenta stained glass");
        addMaterialName(Material.STAINED_GLASS, 3, "light blue stained glass");
        addMaterialName(Material.STAINED_GLASS, 4, "yellow stained glass");
        addMaterialName(Material.STAINED_GLASS, 5, "lime stained glass");
        addMaterialName(Material.STAINED_GLASS, 6, "pink stained glass");
        addMaterialName(Material.STAINED_GLASS, 7, "gray stained glass");
        addMaterialName(Material.STAINED_GLASS, 8, "light gray stained glass");
        addMaterialName(Material.STAINED_GLASS, 9, "cyan stained glass");
        addMaterialName(Material.STAINED_GLASS, 10, "purple stained glass");
        addMaterialName(Material.STAINED_GLASS, 11, "blue stained glass");
        addMaterialName(Material.STAINED_GLASS, 12, "brown stained glass");
        addMaterialName(Material.STAINED_GLASS, 13, "green stained glass");
        addMaterialName(Material.STAINED_GLASS, 14, "red stained glass");
        addMaterialName(Material.STAINED_GLASS, 15, "black stained glass");

        addMaterialName(Material.STAINED_GLASS_PANE, 0, "white stained glass pane");
        addMaterialName(Material.STAINED_GLASS_PANE, 1, "orange stained glass pane");
        addMaterialName(Material.STAINED_GLASS_PANE, 2, "magenta stained glass pane");
        addMaterialName(Material.STAINED_GLASS_PANE, 3, "light blue stained glass pane");
        addMaterialName(Material.STAINED_GLASS_PANE, 4, "yellow stained glass pane");
        addMaterialName(Material.STAINED_GLASS_PANE, 5, "lime stained glass pane");
        addMaterialName(Material.STAINED_GLASS_PANE, 6, "pink stained glass pane");
        addMaterialName(Material.STAINED_GLASS_PANE, 7, "gray stained glass pane");
        addMaterialName(Material.STAINED_GLASS_PANE, 8, "light gray stained glass pane");
        addMaterialName(Material.STAINED_GLASS_PANE, 9, "cyan stained glass pane");
        addMaterialName(Material.STAINED_GLASS_PANE, 10, "purple stained glass pane");
        addMaterialName(Material.STAINED_GLASS_PANE, 11, "blue stained glass pane");
        addMaterialName(Material.STAINED_GLASS_PANE, 12, "brown stained glass pane");
        addMaterialName(Material.STAINED_GLASS_PANE, 13, "green stained glass pane");
        addMaterialName(Material.STAINED_GLASS_PANE, 14, "red stained glass pane");
        addMaterialName(Material.STAINED_GLASS_PANE, 15, "black stained glass pane");

        addMaterialName(Material.STEP, 0, "stone slab");
        addMaterialName(Material.STEP, 1, "sandstone slab");
        addMaterialName(Material.STEP, 2, "wooden slab");
        addMaterialName(Material.STEP, 3, "cobblestone slab");
        addMaterialName(Material.STEP, 4, "brick slab");
        addMaterialName(Material.STEP, 5, "stone brick slab");
        addMaterialName(Material.STEP, 6, "nether brick slab");
        addMaterialName(Material.STEP, 7, "quartz slab");

        addMaterialName(Material.WOOD_STEP, 0, "oak wood slab");
        addMaterialName(Material.WOOD_STEP, 1, "spruce wood slab");
        addMaterialName(Material.WOOD_STEP, 2, "birch wood slab");
        addMaterialName(Material.WOOD_STEP, 3, "jungle wood slab");
        addMaterialName(Material.WOOD_STEP, 4, "acacia wood slab");
        addMaterialName(Material.WOOD_STEP, 5, "dark oak wood slab");

        addMaterialName(Material.LONG_GRASS, 0, "shrub");
        addMaterialName(Material.LONG_GRASS, 1, "tall grass");
        addMaterialName(Material.LONG_GRASS, 2, "fern");

        addMaterialName(Material.RED_ROSE, 0, "poppy");
        addMaterialName(Material.RED_ROSE, 1, "blue orchid");
        addMaterialName(Material.RED_ROSE, 2, "allium");
        addMaterialName(Material.RED_ROSE, 3, "azure bluet");
        addMaterialName(Material.RED_ROSE, 4, "red tulip");
        addMaterialName(Material.RED_ROSE, 5, "orange tulip");
        addMaterialName(Material.RED_ROSE, 6, "white tulip");
        addMaterialName(Material.RED_ROSE, 7, "pink tulip");
        addMaterialName(Material.RED_ROSE, 8, "oxeye daisy");

        addMaterialName(Material.DOUBLE_PLANT, 0, "sunflower");
        addMaterialName(Material.DOUBLE_PLANT, 1, "lilac");
        addMaterialName(Material.DOUBLE_PLANT, 2, "double tall grass");
        addMaterialName(Material.DOUBLE_PLANT, 3, "large fern");
        addMaterialName(Material.DOUBLE_PLANT, 4, "rose bush");
        addMaterialName(Material.DOUBLE_PLANT, 5, "peony");

        addMaterialName(Material.SMOOTH_BRICK, 0, "stone brick");
        addMaterialName(Material.SMOOTH_BRICK, 1, "mossy stone brick");
        addMaterialName(Material.SMOOTH_BRICK, 2, "cracked stone brick");
        addMaterialName(Material.SMOOTH_BRICK, 3, "chiseled stone brick");

        addMaterialName(Material.COBBLE_WALL, 1, "mossy cobble wall");

        addMaterialName(Material.COAL, 1, "charcoal");

        addMaterialName(Material.INK_SACK, 0, "ink sack");
        addMaterialName(Material.INK_SACK, 1, "rose red");
        addMaterialName(Material.INK_SACK, 2, "cactus green");
        addMaterialName(Material.INK_SACK, 3, "cocoa beans");
        addMaterialName(Material.INK_SACK, 4, "lapis lazuli");
        addMaterialName(Material.INK_SACK, 5, "purple dye");
        addMaterialName(Material.INK_SACK, 6, "cyan dye");
        addMaterialName(Material.INK_SACK, 7, "light gray dye");
        addMaterialName(Material.INK_SACK, 8, "gray dye");
        addMaterialName(Material.INK_SACK, 9, "pink dye");
        addMaterialName(Material.INK_SACK, 10, "lime dye");
        addMaterialName(Material.INK_SACK, 11, "dandelion yellow");
        addMaterialName(Material.INK_SACK, 12, "light blue dye");
        addMaterialName(Material.INK_SACK, 13, "magenta dye");
        addMaterialName(Material.INK_SACK, 14, "orange dye");
        addMaterialName(Material.INK_SACK, 15, "bone meal");

        addMaterialName(Material.RAW_FISH, 1, "raw salmon");
        addMaterialName(Material.RAW_FISH, 2, "raw clownfish");
        addMaterialName(Material.RAW_FISH, 3, "pufferfish");

        for (Entry<String, String> entry : MATERIAL_NAMES.entrySet()) {
            yml.set("materials." + entry.getKey(), entry.getValue());
        }

        try {
            yml.save(materialsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addMaterialName(Material material, int data, String name) {
        MATERIAL_NAMES.put(material.name() + ";" + data, name);
    }

    public static String entityName(EntityType entity) {
        return entity.name().toLowerCase().replace('_', ' ');
    }

    public static String entityName(int id) {
        return entityName(EntityType.fromId(id));
    }

    public static String materialName(int id) {
        return materialName(Material.getMaterial(id), (byte) 0);
    }

    public static String materialName(int id, byte data) {
        return materialName(Material.getMaterial(id), data);
    }

    public static String materialName(Material material, byte data) {
        final String key = material.name() + ";" + data;

        if (MATERIAL_NAMES.containsKey(key)) {
            return MATERIAL_NAMES.get(key);
        }

        return toMaterialName(material);
    }

    private static String toMaterialName(Material material) {
        return material.toString().toLowerCase().replace('_', ' ');
    }

}
