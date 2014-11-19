package jcraft.jblockactivity.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jcraft.jblockactivity.BlockActivity;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.material.MaterialData;

public class MaterialAliases {

    private static Map<MaterialData, String[]> MATERIAL_ALIASES = new LinkedHashMap<MaterialData, String[]>();

    public static void loadMaterialAliases() {
        final File aliasesFile = new File(BlockActivity.DATA_FOLDER, "aliases.yml");

        if (!aliasesFile.exists()) {
            MATERIAL_ALIASES = new LinkedHashMap<MaterialData, String[]>();
            saveMaterialAliases();
        }

        MATERIAL_ALIASES = new HashMap<MaterialData, String[]>();

        final YamlConfiguration yml = YamlConfiguration.loadConfiguration(aliasesFile);

        final Set<String> materials = yml.getConfigurationSection("aliases").getKeys(false);

        for (String key : materials) {
            int id, data;

            try {
                id = Integer.parseInt(key.split(";")[0]);
                data = Integer.parseInt(key.split(";")[1]);
            } catch (NumberFormatException e) {
                continue;
            }

            List<String> aliases = yml.getStringList("aliases." + key);

            MATERIAL_ALIASES.put(new MaterialData(id, (byte) data), aliases.toArray(new String[aliases.size()]));
        }
    }

    public static void saveMaterialAliases() {
        final File aliasesFile = new File(BlockActivity.DATA_FOLDER, "aliases.yml");
        final YamlConfiguration yml = YamlConfiguration.loadConfiguration(aliasesFile);

        addMaterialName(Material.WOOD, 1, "sprucewoodplanks", "sprucewood");
        addMaterialName(Material.WOOD, 2, "birchwoodplanks", "birchwood");
        addMaterialName(Material.WOOD, 3, "junglewoodplanks", "junglewood");
        addMaterialName(Material.WOOD, 4, "acaciawoodplanks", "acaciawood");
        addMaterialName(Material.WOOD, 5, "darkoakwoodplanks", "darkoakwood");

        addMaterialName(Material.DIRT, 1, "coarsedirt", "grasslessdirt");
        addMaterialName(Material.DIRT, 2, "podzol");

        addMaterialName(Material.SAPLING, 0, "oaksapling");
        addMaterialName(Material.SAPLING, 1, "sprucesapling");
        addMaterialName(Material.SAPLING, 2, "birchsapling");
        addMaterialName(Material.SAPLING, 3, "junglesapling");
        addMaterialName(Material.SAPLING, 4, "acaciasapling");
        addMaterialName(Material.SAPLING, 5, "darkoaksapling");

        addMaterialName(Material.SAND, 1, "redsand");

        addMaterialName(Material.LOG, 0, "oaklog");
        addMaterialName(Material.LOG, 1, "sprucelog");
        addMaterialName(Material.LOG, 2, "birchlog");
        addMaterialName(Material.LOG, 3, "junglelog");

        addMaterialName(Material.LOG_2, 0, "acacialog");
        addMaterialName(Material.LOG_2, 1, "darkoaklog");

        addMaterialName(Material.LEAVES, 0, "oakleaves");
        addMaterialName(Material.LEAVES, 1, "spruceleaves");
        addMaterialName(Material.LEAVES, 2, "birchleaves");
        addMaterialName(Material.LEAVES, 3, "jungleleaves");

        addMaterialName(Material.LEAVES_2, 0, "acacialeaves");
        addMaterialName(Material.LEAVES_2, 1, "darkoakleaves");

        addMaterialName(Material.WOOL, 0, "whitewool");
        addMaterialName(Material.WOOL, 1, "orangewool");
        addMaterialName(Material.WOOL, 2, "magentawool");
        addMaterialName(Material.WOOL, 3, "lightbluewool");
        addMaterialName(Material.WOOL, 4, "yellowwool");
        addMaterialName(Material.WOOL, 5, "limewool");
        addMaterialName(Material.WOOL, 6, "pinkwool");
        addMaterialName(Material.WOOL, 7, "graywool");
        addMaterialName(Material.WOOL, 8, "lightgraywool");
        addMaterialName(Material.WOOL, 9, "cyanwool");
        addMaterialName(Material.WOOL, 10, "purplewool");
        addMaterialName(Material.WOOL, 11, "bluewool");
        addMaterialName(Material.WOOL, 12, "brownwool");
        addMaterialName(Material.WOOL, 13, "greenwool");
        addMaterialName(Material.WOOL, 14, "redwool");
        addMaterialName(Material.WOOL, 15, "blackwool");

        addMaterialName(Material.CARPET, 0, "whitecarpet");
        addMaterialName(Material.CARPET, 1, "orangecarpet");
        addMaterialName(Material.CARPET, 2, "magentacarpet");
        addMaterialName(Material.CARPET, 3, "lightbluecarpet");
        addMaterialName(Material.CARPET, 4, "yellowcarpet");
        addMaterialName(Material.CARPET, 5, "limecarpet");
        addMaterialName(Material.CARPET, 6, "pinkcarpet");
        addMaterialName(Material.CARPET, 7, "graycarpet");
        addMaterialName(Material.CARPET, 8, "lightgraycarpet");
        addMaterialName(Material.CARPET, 9, "cyancarpet");
        addMaterialName(Material.CARPET, 10, "purplecarpet");
        addMaterialName(Material.CARPET, 11, "bluecarpet");
        addMaterialName(Material.CARPET, 12, "browncarpet");
        addMaterialName(Material.CARPET, 13, "greencarpet");
        addMaterialName(Material.CARPET, 14, "redcarpet");
        addMaterialName(Material.CARPET, 15, "blackcarpet");

        addMaterialName(Material.STAINED_CLAY, 0, "whitestainedclay");
        addMaterialName(Material.STAINED_CLAY, 1, "orangestainedclay");
        addMaterialName(Material.STAINED_CLAY, 2, "magentastainedclay");
        addMaterialName(Material.STAINED_CLAY, 3, "lightbluestainedclay");
        addMaterialName(Material.STAINED_CLAY, 4, "yellowstainedclay");
        addMaterialName(Material.STAINED_CLAY, 5, "limestainedclay");
        addMaterialName(Material.STAINED_CLAY, 6, "pinkstainedclay");
        addMaterialName(Material.STAINED_CLAY, 7, "graystainedclay");
        addMaterialName(Material.STAINED_CLAY, 8, "lightgraystainedclay");
        addMaterialName(Material.STAINED_CLAY, 9, "cyanstainedclay");
        addMaterialName(Material.STAINED_CLAY, 10, "purplestainedclay");
        addMaterialName(Material.STAINED_CLAY, 11, "bluestainedclay");
        addMaterialName(Material.STAINED_CLAY, 12, "brownstainedclay");
        addMaterialName(Material.STAINED_CLAY, 13, "greenstainedclay");
        addMaterialName(Material.STAINED_CLAY, 14, "redstainedclay");
        addMaterialName(Material.STAINED_CLAY, 15, "blackstainedclay");

        addMaterialName(Material.STAINED_GLASS, 0, "whitestainedglass");
        addMaterialName(Material.STAINED_GLASS, 1, "orangestainedglass");
        addMaterialName(Material.STAINED_GLASS, 2, "magentastainedglass");
        addMaterialName(Material.STAINED_GLASS, 3, "lightbluestainedglass");
        addMaterialName(Material.STAINED_GLASS, 4, "yellowstainedglass");
        addMaterialName(Material.STAINED_GLASS, 5, "limestainedglass");
        addMaterialName(Material.STAINED_GLASS, 6, "pinkstainedglass");
        addMaterialName(Material.STAINED_GLASS, 7, "graystainedglass");
        addMaterialName(Material.STAINED_GLASS, 8, "lightgraystainedglass");
        addMaterialName(Material.STAINED_GLASS, 9, "cyanstainedglass");
        addMaterialName(Material.STAINED_GLASS, 10, "purplestainedglass");
        addMaterialName(Material.STAINED_GLASS, 11, "bluestainedglass");
        addMaterialName(Material.STAINED_GLASS, 12, "brownstainedglass");
        addMaterialName(Material.STAINED_GLASS, 13, "greenstainedglass");
        addMaterialName(Material.STAINED_GLASS, 14, "redstainedglass");
        addMaterialName(Material.STAINED_GLASS, 15, "blackstainedglass");

        addMaterialName(Material.STAINED_GLASS_PANE, 0, "whitestainedglasspane", "whitepane");
        addMaterialName(Material.STAINED_GLASS_PANE, 1, "orangestainedglasspane", "orangepane");
        addMaterialName(Material.STAINED_GLASS_PANE, 2, "magentastainedglasspane", "magentapane");
        addMaterialName(Material.STAINED_GLASS_PANE, 3, "lightbluestainedglasspane", "lightbluepane");
        addMaterialName(Material.STAINED_GLASS_PANE, 4, "yellowstainedglasspane", "yellowpane");
        addMaterialName(Material.STAINED_GLASS_PANE, 5, "limestainedglasspane", "limepane");
        addMaterialName(Material.STAINED_GLASS_PANE, 6, "pinkstainedglasspane", "pinkpane");
        addMaterialName(Material.STAINED_GLASS_PANE, 7, "graystainedglasspane", "graypane");
        addMaterialName(Material.STAINED_GLASS_PANE, 8, "lightgraystainedglasspane", "lightgraypane");
        addMaterialName(Material.STAINED_GLASS_PANE, 9, "cyanstainedglasspane", "cyanpane");
        addMaterialName(Material.STAINED_GLASS_PANE, 10, "purplestainedglasspane", "purplepane");
        addMaterialName(Material.STAINED_GLASS_PANE, 11, "bluestainedglasspane", "bluepane");
        addMaterialName(Material.STAINED_GLASS_PANE, 12, "brownstainedglasspane", "brownpane");
        addMaterialName(Material.STAINED_GLASS_PANE, 13, "greenstainedglasspane", "greenpane");
        addMaterialName(Material.STAINED_GLASS_PANE, 14, "redstainedglasspane", "redpane");
        addMaterialName(Material.STAINED_GLASS_PANE, 15, "blackstainedglasspane", "blackpane");

        addMaterialName(Material.STEP, 0, "stoneslab");
        addMaterialName(Material.STEP, 1, "sandstoneslab");
        addMaterialName(Material.STEP, 2, "woodenslab");
        addMaterialName(Material.STEP, 3, "cobblestoneslab");
        addMaterialName(Material.STEP, 4, "brickslab");
        addMaterialName(Material.STEP, 5, "stonebrickslab");
        addMaterialName(Material.STEP, 6, "netherbrickslab");
        addMaterialName(Material.STEP, 7, "quartzslab");

        addMaterialName(Material.WOOD_STEP, 0, "oakwoodslab", "oakslab");
        addMaterialName(Material.WOOD_STEP, 1, "sprucewoodslab", "spruceslab");
        addMaterialName(Material.WOOD_STEP, 2, "birchwoodslab", "birchslab");
        addMaterialName(Material.WOOD_STEP, 3, "junglewoodslab", "jungleslab");
        addMaterialName(Material.WOOD_STEP, 4, "acaciawoodslab", "acaciaslab");
        addMaterialName(Material.WOOD_STEP, 5, "darkoakwoodslab", "darkoakslab");

        addMaterialName(Material.LONG_GRASS, 0, "shrub");
        addMaterialName(Material.LONG_GRASS, 1, "tallgrass");
        addMaterialName(Material.LONG_GRASS, 2, "fern");

        addMaterialName(Material.RED_ROSE, 0, "poppy");
        addMaterialName(Material.RED_ROSE, 1, "blueorchid");
        addMaterialName(Material.RED_ROSE, 2, "allium");
        addMaterialName(Material.RED_ROSE, 3, "azurebluet");
        addMaterialName(Material.RED_ROSE, 4, "redtulip");
        addMaterialName(Material.RED_ROSE, 5, "orangetulip");
        addMaterialName(Material.RED_ROSE, 6, "whitetulip");
        addMaterialName(Material.RED_ROSE, 7, "pinktulip");
        addMaterialName(Material.RED_ROSE, 8, "oxeyedaisy");

        addMaterialName(Material.DOUBLE_PLANT, 0, "sunflower");
        addMaterialName(Material.DOUBLE_PLANT, 1, "lilac");
        addMaterialName(Material.DOUBLE_PLANT, 2, "doubletallgrass");
        addMaterialName(Material.DOUBLE_PLANT, 3, "largefern");
        addMaterialName(Material.DOUBLE_PLANT, 4, "rosebush");
        addMaterialName(Material.DOUBLE_PLANT, 5, "peony");

        addMaterialName(Material.SMOOTH_BRICK, 0, "stonebrick");
        addMaterialName(Material.SMOOTH_BRICK, 1, "mossystonebrick");
        addMaterialName(Material.SMOOTH_BRICK, 2, "crackedstonebrick");
        addMaterialName(Material.SMOOTH_BRICK, 3, "chiseledstonebrick");

        addMaterialName(Material.COBBLE_WALL, 1, "mossycobblewall");

        addMaterialName(Material.COAL, 1, "charcoal");

        addMaterialName(Material.INK_SACK, 0, "inksack");
        addMaterialName(Material.INK_SACK, 1, "rosered");
        addMaterialName(Material.INK_SACK, 2, "cactusgreen");
        addMaterialName(Material.INK_SACK, 3, "cocoabeans");
        addMaterialName(Material.INK_SACK, 4, "lapislazuli");
        addMaterialName(Material.INK_SACK, 5, "purpledye");
        addMaterialName(Material.INK_SACK, 6, "cyandye");
        addMaterialName(Material.INK_SACK, 7, "lightgraydye");
        addMaterialName(Material.INK_SACK, 8, "graydye");
        addMaterialName(Material.INK_SACK, 9, "pinkdye");
        addMaterialName(Material.INK_SACK, 10, "limedye");
        addMaterialName(Material.INK_SACK, 11, "dandelionyellow");
        addMaterialName(Material.INK_SACK, 12, "lightbluedye");
        addMaterialName(Material.INK_SACK, 13, "magentadye");
        addMaterialName(Material.INK_SACK, 14, "orangedye");
        addMaterialName(Material.INK_SACK, 15, "bonemeal");

        addMaterialName(Material.RAW_FISH, 1, "rawsalmon");
        addMaterialName(Material.RAW_FISH, 2, "rawclownfish");
        addMaterialName(Material.RAW_FISH, 3, "pufferfish");

        for (Entry<MaterialData, String[]> entry : MATERIAL_ALIASES.entrySet()) {
            yml.set("aliases." + (entry.getKey().getItemType().getId() + ";" + entry.getKey().getData()), entry.getValue());
        }

        try {
            yml.save(aliasesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addMaterialName(Material material, int data, String... name) {
        MATERIAL_ALIASES.put(new MaterialData(material, (byte) data), name);
    }

    public static MaterialData matchMaterial(String name) {
        Material material;

        // Bukkit material matching
        material = Material.matchMaterial(name);

        if (material != null) {
            return new MaterialData(material, (byte) -1);
        }

        // BlockActivity alias matching
        for (Entry<MaterialData, String[]> entry : MATERIAL_ALIASES.entrySet()) {
            for (String alias : entry.getValue()) {
                if (alias.equalsIgnoreCase(name)) {
                    return entry.getKey();
                }
            }
        }

        return null;
    }

}
