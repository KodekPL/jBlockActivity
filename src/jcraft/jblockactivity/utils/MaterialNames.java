package jcraft.jblockactivity.utils;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class MaterialNames {

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
        switch (material) {
        case WOOD:
            if (data == 0) {
                return "oak wood planks";
            } else if (data == 1) {
                return "spruce wood planks";
            } else if (data == 2) {
                return "birch wood planks";
            } else if (data == 3) {
                return "jungle wood planks";
            } else if (data == 4) {
                return "acacia wood planks";
            } else if (data == 5) {
                return "dark oak wood planks";
            }
            break;
        case DIRT:
            if (data == 1) {
                return "grassless dirt";
            } else if (data == 2) {
                return "podzol";
            }
            break;
        case SAPLING:
            if (data == 0) {
                return "oak sapling";
            } else if (data == 1) {
                return "spruce sapling";
            } else if (data == 2) {
                return "birch sapling";
            } else if (data == 3) {
                return "jungle sapling";
            } else if (data == 4) {
                return "acacia sapling";
            } else if (data == 5) {
                return "dark oak sapling";
            }
            break;
        case SAND:
            if (data == 1) {
                return "red sand";
            }
            break;
        case LOG:
            if (data == 0 || data == 4 || data == 8 || data == 12) {
                return "oak log";
            } else if (data == 1 || data == 5 || data == 9 || data == 13) {
                return "spruce log";
            } else if (data == 2 || data == 6 || data == 10 || data == 14) {
                return "birch log";
            } else if (data == 3 || data == 7 || data == 11 || data == 15) {
                return "jungle log";
            }
            break;
        case LOG_2:
            if (data == 0 || data == 4 || data == 8) {
                return "acacia log";
            } else if (data == 1 || data == 5 || data == 9) {
                return "dark oak log";
            }
            break;
        case LEAVES:
            if (data == 0) {
                return "oak leaves";
            } else if (data == 1) {
                return "spruce leaves";
            } else if (data == 2) {
                return "birch leaves";
            } else if (data == 3) {
                return "jungle leaves";
            }
            break;
        case LEAVES_2:
            if (data == 0) {
                return "acacia leaves";
            } else if (data == 1) {
                return "dark oak leaves";
            }
            break;
        case WOOL:
            if (data == 0) {
                return "white wool";
            } else if (data == 1) {
                return "orange wool";
            } else if (data == 2) {
                return "magenta wool";
            } else if (data == 3) {
                return "light blue wool";
            } else if (data == 4) {
                return "yellow wool";
            } else if (data == 5) {
                return "lime wool";
            } else if (data == 6) {
                return "pink wool";
            } else if (data == 7) {
                return "gray wool";
            } else if (data == 8) {
                return "light gray wool";
            } else if (data == 9) {
                return "cyan wool";
            } else if (data == 10) {
                return "purple wool";
            } else if (data == 11) {
                return "blue wool";
            } else if (data == 12) {
                return "brown wool";
            } else if (data == 13) {
                return "green wool";
            } else if (data == 14) {
                return "red wool";
            } else if (data == 15) {
                return "black wool";
            }
            break;
        case CARPET:
            if (data == 0) {
                return "white carpet";
            } else if (data == 1) {
                return "orange carpet";
            } else if (data == 2) {
                return "magenta carpet";
            } else if (data == 3) {
                return "light blue carpet";
            } else if (data == 4) {
                return "yellow carpet";
            } else if (data == 5) {
                return "lime carpet";
            } else if (data == 6) {
                return "pink carpet";
            } else if (data == 7) {
                return "gray carpet";
            } else if (data == 8) {
                return "light gray carpet";
            } else if (data == 9) {
                return "cyan carpet";
            } else if (data == 10) {
                return "purple carpet";
            } else if (data == 11) {
                return "blue carpet";
            } else if (data == 12) {
                return "brown carpet";
            } else if (data == 13) {
                return "green carpet";
            } else if (data == 14) {
                return "red carpet";
            } else if (data == 15) {
                return "black carpet";
            }
            break;
        case STAINED_CLAY:
            if (data == 0) {
                return "white stained clay";
            } else if (data == 1) {
                return "orange stained clay";
            } else if (data == 2) {
                return "magenta stained clay";
            } else if (data == 3) {
                return "light blue stained clay";
            } else if (data == 4) {
                return "yellow stained clay";
            } else if (data == 5) {
                return "lime stained clay";
            } else if (data == 6) {
                return "pink stained clay";
            } else if (data == 7) {
                return "gray stained clay";
            } else if (data == 8) {
                return "light gray stained clay";
            } else if (data == 9) {
                return "cyan stained clay";
            } else if (data == 10) {
                return "purple stained clay";
            } else if (data == 11) {
                return "blue stained clay";
            } else if (data == 12) {
                return "brown stained clay";
            } else if (data == 13) {
                return "green stained clay";
            } else if (data == 14) {
                return "red stained clay";
            } else if (data == 15) {
                return "black stained clay";
            }
            break;
        case STAINED_GLASS:
            if (data == 0) {
                return "white stained glass";
            } else if (data == 1) {
                return "orange stained glass";
            } else if (data == 2) {
                return "magenta stained glass";
            } else if (data == 3) {
                return "light blue stained glass";
            } else if (data == 4) {
                return "yellow stained glass";
            } else if (data == 5) {
                return "lime stained glass";
            } else if (data == 6) {
                return "pink stained glass";
            } else if (data == 7) {
                return "gray stained glass";
            } else if (data == 8) {
                return "light gray stained glass";
            } else if (data == 9) {
                return "cyan stained glass";
            } else if (data == 10) {
                return "purple stained glass";
            } else if (data == 11) {
                return "blue stained glass";
            } else if (data == 12) {
                return "brown stained glass";
            } else if (data == 13) {
                return "green stained glass";
            } else if (data == 14) {
                return "red stained glass";
            } else if (data == 15) {
                return "black stained glass";
            }
            break;
        case STAINED_GLASS_PANE:
            if (data == 0) {
                return "white stained glass pane";
            } else if (data == 1) {
                return "orange stained glass pane";
            } else if (data == 2) {
                return "magenta stained glass pane";
            } else if (data == 3) {
                return "light blue stained glass pane";
            } else if (data == 4) {
                return "yellow stained glass pane";
            } else if (data == 5) {
                return "lime stained glass pane";
            } else if (data == 6) {
                return "pink stained glass pane";
            } else if (data == 7) {
                return "gray stained glass pane";
            } else if (data == 8) {
                return "light gray stained glass pane";
            } else if (data == 9) {
                return "cyan stained glass pane";
            } else if (data == 10) {
                return "purple stained glass pane";
            } else if (data == 11) {
                return "blue stained glass pane";
            } else if (data == 12) {
                return "brown stained glass pane";
            } else if (data == 13) {
                return "green stained glass pane";
            } else if (data == 14) {
                return "red stained glass pane";
            } else if (data == 15) {
                return "black stained glass pane";
            }
            break;
        case STEP:
            if (data == 0) {
                return "stone slab";
            } else if (data == 1) {
                return "sandstone slab";
            } else if (data == 2) {
                return "wooden slab";
            } else if (data == 3) {
                return "cobblestone slab";
            } else if (data == 4) {
                return "brick slab";
            } else if (data == 5) {
                return "stone brick slab";
            } else if (data == 6) {
                return "nether brick slab";
            } else if (data == 7) {
                return "quartz slab";
            }
            break;
        case WOOD_STEP:
            if (data == 0) {
                return "oak wood slab";
            } else if (data == 1) {
                return "spruce wood slab";
            } else if (data == 2) {
                return "birch wood slab";
            } else if (data == 3) {
                return "jungle wood slab";
            } else if (data == 4) {
                return "acacia wood slab";
            } else if (data == 5) {
                return "dark oak wood slab";
            }
            break;
        case LONG_GRASS:
            if (data == 0) {
                return "shrub";
            } else if (data == 1) {
                return "tall grass";
            } else if (data == 2) {
                return "fern";
            }
            break;
        case RED_ROSE:
            if (data == 0) {
                return "poppy";
            } else if (data == 1) {
                return "blue orchid";
            } else if (data == 2) {
                return "allium";
            } else if (data == 3) {
                return "azure bluet";
            } else if (data == 4) {
                return "red tulip";
            } else if (data == 5) {
                return "orange tulip";
            } else if (data == 6) {
                return "white tulip";
            } else if (data == 7) {
                return "pink tulip";
            } else if (data == 8) {
                return "oxeye daisy";
            }
            break;
        case DOUBLE_PLANT:
            if (data == 0) {
                return "sunflower";
            } else if (data == 1) {
                return "lilac";
            } else if (data == 2) {
                return "double tall grass";
            } else if (data == 3) {
                return "large fern";
            } else if (data == 4) {
                return "rose bush";
            } else if (data == 5) {
                return "peony";
            }
            break;
        case SMOOTH_BRICK:
            if (data == 0) {
                return "stone brick";
            } else if (data == 1) {
                return "mossy stone brick";
            } else if (data == 2) {
                return "cracked stone brick";
            } else if (data == 3) {
                return "chiseled stone brick";
            }
            break;
        case COBBLE_WALL:
            if (data == 1) {
                return "mossy cobble wall";
            }
            break;
        case COAL:
            if (data == 1) {
                return "charcoal";
            }
            break;
        case INK_SACK:
            if (data == 0) {
                return "ink sack";
            } else if (data == 1) {
                return "rose red";
            } else if (data == 2) {
                return "cactus green";
            } else if (data == 3) {
                return "cocoa beans";
            } else if (data == 4) {
                return "lapis lazuli";
            } else if (data == 5) {
                return "purple dye";
            } else if (data == 6) {
                return "cyan dye";
            } else if (data == 7) {
                return "light gray dye";
            } else if (data == 8) {
                return "gray dye";
            } else if (data == 9) {
                return "pink dye";
            } else if (data == 10) {
                return "lime dye";
            } else if (data == 11) {
                return "dandelion yellow";
            } else if (data == 12) {
                return "light blue dye";
            } else if (data == 13) {
                return "magenta dye";
            } else if (data == 14) {
                return "orange dye";
            } else if (data == 15) {
                return "bone meal";
            }
            break;
        case RAW_FISH:
            if (data == 1) {
                return "raw salmon";
            } else if (data == 2) {
                return "raw clownfish";
            } else if (data == 3) {
                return "pufferfish";
            }
            break;
        default:
            break;
        }
        return toMaterialName(material);
    }

    private static String toMaterialName(Material material) {
        return material.toString().toLowerCase().replace('_', ' ');
    }

}
