package jcraft.jblockactivity.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class BlocksUtil {

    private static final Set<Set<Integer>> BLOCK_EQUIVALENTS;

    static {
        BLOCK_EQUIVALENTS = new HashSet<Set<Integer>>(9);
        BLOCK_EQUIVALENTS.add(new HashSet<Integer>(Arrays.asList(2, 3, 60)));
        BLOCK_EQUIVALENTS.add(new HashSet<Integer>(Arrays.asList(8, 9, 79)));
        BLOCK_EQUIVALENTS.add(new HashSet<Integer>(Arrays.asList(10, 11)));
        BLOCK_EQUIVALENTS.add(new HashSet<Integer>(Arrays.asList(61, 62)));
        BLOCK_EQUIVALENTS.add(new HashSet<Integer>(Arrays.asList(73, 74)));
        BLOCK_EQUIVALENTS.add(new HashSet<Integer>(Arrays.asList(75, 76)));
        BLOCK_EQUIVALENTS.add(new HashSet<Integer>(Arrays.asList(93, 94)));
        BLOCK_EQUIVALENTS.add(new HashSet<Integer>(Arrays.asList(123, 124)));
        BLOCK_EQUIVALENTS.add(new HashSet<Integer>(Arrays.asList(149, 150)));
    }

    public static boolean isEqualType(int type1, int type2) {
        if (type1 == type2) {
            return true;
        }
        for (final Set<Integer> equivalent : BLOCK_EQUIVALENTS) {
            if (equivalent.contains(type1) && equivalent.contains(type2)) {
                return true;
            }
        }
        return false;
    }

    public static int safeSpawnHeight(Location location) {
        final World world = location.getWorld();
        final Chunk chunk = world.getChunkAt(location);
        if (!world.isChunkLoaded(chunk)) {
            world.loadChunk(chunk);
        }
        final int x = location.getBlockX();
        final int z = location.getBlockZ();
        int y = location.getBlockY();
        boolean lower = world.getBlockAt(x, y, z).getType() == Material.AIR;
        boolean upper = world.getBlockAt(x, y + 1, z).getType() == Material.AIR;

        while ((!lower || !upper) && y != 255) {
            lower = upper;
            upper = world.getBlockAt(x, ++y, z).getType() == Material.AIR;
        }
        while (world.getBlockAt(x, y - 1, z).getType() == Material.AIR && y != 0) {
            y--;
        }
        return y;
    }

    public static boolean isEntitySpawnSafe(Material blockId) {
        switch (blockId) {
        case AIR:
        case SAPLING:
        case LONG_GRASS:
        case DEAD_BUSH:
        case YELLOW_FLOWER:
        case RED_ROSE:
        case BROWN_MUSHROOM:
        case RED_MUSHROOM:
        case REDSTONE_WIRE:
        case LEVER:
        case STONE_BUTTON:
        case REDSTONE_TORCH_ON:
        case REDSTONE_TORCH_OFF:
        case SNOW:
        case VINE:
        case CARPET:
        case DOUBLE_PLANT:
            return true;
        default:
            return false;
        }
    }

    public static boolean hasExtraData(Material material) {
        switch (material) {
        case WALL_SIGN:
        case SIGN_POST:
        case COMMAND:
        case MOB_SPAWNER:
        case FLOWER_POT:
        case SKULL:
            return true;
        default:
            return false;
        }
    }

    public static boolean isFallingBlock(Material material) {
        switch (material) {
        case SAND:
        case GRAVEL:
        case DRAGON_EGG:
        case ANVIL:
            return true;
        default:
            return false;
        }
    }

    public static boolean isFallingOverridingBlock(Material material) {
        switch (material) {
        case AIR:
        case WATER:
        case STATIONARY_WATER:
        case LAVA:
        case STATIONARY_LAVA:
        case FIRE:
        case VINE:
        case LONG_GRASS:
        case DEAD_BUSH:
        case SNOW:
            return true;
        default:
            return false;
        }
    }

    public static boolean isFallingBlockKiller(Material material) {
        switch (material) {
        case SIGN_POST:
        case WALL_SIGN:
        case STONE_PLATE:
        case WOOD_PLATE:
        case IRON_PLATE:
        case GOLD_PLATE:
        case SAPLING:
        case SUGAR_CANE_BLOCK:
        case RED_ROSE:
        case YELLOW_FLOWER:
        case CROPS:
        case POTATO:
        case CARROT:
        case WATER_LILY:
        case RED_MUSHROOM:
        case BROWN_MUSHROOM:
        case STEP:
        case WOOD_STEP:
        case TORCH:
        case FLOWER_POT:
        case POWERED_RAIL:
        case DETECTOR_RAIL:
        case ACTIVATOR_RAIL:
        case RAILS:
        case LEVER:
        case REDSTONE_WIRE:
        case REDSTONE_TORCH_ON:
        case REDSTONE_TORCH_OFF:
        case DIODE_BLOCK_ON:
        case DIODE_BLOCK_OFF:
        case REDSTONE_COMPARATOR_ON:
        case REDSTONE_COMPARATOR_OFF:
        case DAYLIGHT_DETECTOR:
        case CARPET:
        case DOUBLE_PLANT:
            return true;
        default:
            return false;
        }
    }

    public static boolean isContainerBlock(Material material) {
        switch (material) {
        case CHEST:
        case TRAPPED_CHEST:
        case DISPENSER:
        case DROPPER:
        case HOPPER:
        case BREWING_STAND:
        case FURNACE:
        case BURNING_FURNACE:
        case BEACON:
            return true;
        default:
            return false;
        }
    }

    public static boolean isBottomRelativeBreakableBlock(Material material) {
        switch (material) {
        case SAPLING:
        case LONG_GRASS:
        case DEAD_BUSH:
        case YELLOW_FLOWER:
        case RED_ROSE:
        case BROWN_MUSHROOM:
        case RED_MUSHROOM:
        case CROPS:
        case POTATO:
        case CARROT:
        case WATER_LILY:
        case CACTUS:
        case SUGAR_CANE_BLOCK:
        case FLOWER_POT:
        case POWERED_RAIL:
        case DETECTOR_RAIL:
        case ACTIVATOR_RAIL:
        case RAILS:
        case REDSTONE_WIRE:
        case SIGN_POST:
        case STONE_PLATE:
        case WOOD_PLATE:
        case IRON_PLATE:
        case GOLD_PLATE:
        case SNOW:
        case DIODE_BLOCK_ON:
        case DIODE_BLOCK_OFF:
        case REDSTONE_COMPARATOR_ON:
        case REDSTONE_COMPARATOR_OFF:
        case WOODEN_DOOR:
        case IRON_DOOR_BLOCK:
        case CARPET:
        case DOUBLE_PLANT:
            return true;
        default:
            return false;
        }
    }

    public static boolean isSideRelativeBreakableBlock(Material material) {
        switch (material) {
        case WALL_SIGN:
        case LADDER:
        case STONE_BUTTON:
        case WOOD_BUTTON:
        case REDSTONE_TORCH_ON:
        case REDSTONE_TORCH_OFF:
        case LEVER:
        case TORCH:
        case TRAP_DOOR:
        case TRIPWIRE_HOOK:
        case COCOA:
        case BED_BLOCK:
        case PISTON_BASE:
        case PISTON_STICKY_BASE:
        case PISTON_EXTENSION:
            return true;
        default:
            return false;
        }
    }

    private static final BlockFace[] SIDE_RELATIVE_BLOCK_FACES = new BlockFace[] { BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH,
            BlockFace.UP, BlockFace.DOWN };

    public static List<Block> getSideRelativeBreakableBlocks(Block originBlock) {
        final List<Block> blocks = new ArrayList<Block>();
        for (BlockFace blockFace : SIDE_RELATIVE_BLOCK_FACES) {
            Block block = originBlock.getRelative(blockFace);
            if (isSideRelativeBreakableBlock(block.getType())) {
                blocks.add(block);
            }
        }
        return blocks;
    }

    public static final BlockFace[] PRIMARY_CARDINAL_DIRS = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };

    public static BlockFace yawToFace(float yaw) {
        return PRIMARY_CARDINAL_DIRS[Math.round(yaw / 90f) & 0x3];
    }

    public static BlockFace turnFace(BlockFace face, boolean right) {
        int dir = face.ordinal();
        if (right) {
            dir++;
            if (dir >= PRIMARY_CARDINAL_DIRS.length) {
                dir = 0;
            }
        } else {
            dir--;
            if (dir < 0) {
                dir = PRIMARY_CARDINAL_DIRS.length - 1;
            }
        }
        return PRIMARY_CARDINAL_DIRS[dir];
    }

}
