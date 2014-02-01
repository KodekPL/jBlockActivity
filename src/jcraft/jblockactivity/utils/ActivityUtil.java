package jcraft.jblockactivity.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jcraft.jblockactivity.BlockActivity;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class ActivityUtil {

    public static String getEntityName(Entity remover) {
        final String name;
        if (remover instanceof Player) {
            name = ((Player) remover).getName();
        } else if (remover instanceof Projectile && ((Projectile) remover).getShooter() != null) {
            final LivingEntity shooter = ((Projectile) remover).getShooter();
            if (shooter instanceof Player) {
                name = ((Player) shooter).getName();
            } else {
                name = "BA_" + shooter.getType().name().replace('_', ' ').toUpperCase();
            }
        } else {
            name = "BA_" + remover.getType().name().replace('_', ' ').toUpperCase();
        }
        return name;
    }

    public static boolean isSameLocation(Vector v1, Vector v2) {
        if (v1.getBlockX() != v1.getBlockX()) return false;
        if (v1.getBlockY() != v2.getBlockY()) return false;
        if (v1.getBlockZ() != v2.getBlockZ()) return false;
        return true;
    }

    public static int modifyContainer(BlockState block, ItemStack item) {
        if (block instanceof InventoryHolder) {
            final Inventory inv = ((InventoryHolder) block).getInventory();
            if (item.getAmount() < 0) {
                item.setAmount(-item.getAmount());
                final ItemStack tmp = inv.removeItem(item).get(0);
                return (tmp != null) ? tmp.getAmount() : 0;
            } else if (item.getAmount() > 0) {
                final ItemStack tmp = inv.addItem(item).get(0);
                return (tmp != null) ? tmp.getAmount() : 0;
            }
        }
        return 0;
    }

    public static Location getInventoryHolderLocation(InventoryHolder holder) {
        if (holder instanceof DoubleChest) {
            return ((DoubleChest) holder).getLocation();
        } else if (holder instanceof BlockState) {
            return ((BlockState) holder).getLocation();
        } else {
            return null;
        }
    }

    public static Material getInventoryHolderType(InventoryHolder holder) {
        if (holder instanceof DoubleChest) {
            return ((DoubleChest) holder).getLocation().getBlock().getType();
        } else if (holder instanceof BlockState) {
            return ((BlockState) holder).getType();
        } else {
            return null;
        }
    }

    private static final Set<Set<Integer>> blockEquivalents;

    static {
        blockEquivalents = new HashSet<Set<Integer>>(9);
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(2, 3, 60)));
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(8, 9, 79)));
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(10, 11)));
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(61, 62)));
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(73, 74)));
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(75, 76)));
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(93, 94)));
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(123, 124)));
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(149, 150)));
    }

    public static boolean isEqualType(int type1, int type2) {
        if (type1 == type2) {
            return true;
        }
        for (final Set<Integer> equivalent : blockEquivalents) {
            if (equivalent.contains(type1) && equivalent.contains(type2)) {
                return true;
            }
        }
        return false;
    }

    public static int saveSpawnHeight(Location location) {
        final World world = location.getWorld();
        final Chunk chunk = world.getChunkAt(location);
        if (!world.isChunkLoaded(chunk)) {
            world.loadChunk(chunk);
        }
        final int x = location.getBlockX();
        final int z = location.getBlockZ();
        int y = location.getBlockY();
        boolean lower = world.getBlockTypeIdAt(x, y, z) == 0;
        boolean upper = world.getBlockTypeIdAt(x, y + 1, z) == 0;

        while ((!lower || !upper) && y != 255) {
            lower = upper;
            upper = world.getBlockTypeIdAt(x, ++y, z) == 0;
        }
        while (world.getBlockTypeIdAt(x, y - 1, z) == 0 && y != 0) {
            y--;
        }
        return y;
    }

    public static String makeSpaces(int count) {
        final StringBuilder filled = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            filled.append(' ');
        }
        return filled.toString();
    }

    public static int parseTime(String[] sTime) {
        if (sTime == null || sTime.length < 1 || sTime.length > 2) {
            return -1;
        }
        if (sTime.length == 1 && isInt(sTime[0])) {
            return Integer.valueOf(sTime[0]);
        }
        if (!sTime[0].contains(":") && !sTime[0].contains(".")) {
            if (sTime.length == 2) {
                if (!isInt(sTime[0])) {
                    return -1;
                }
                int min = Integer.parseInt(sTime[0]);
                if (sTime[1].charAt(0) == 'h') {
                    min *= 60;
                } else if (sTime[1].charAt(0) == 'd') {
                    min *= 1440;
                }
                return min;
            } else if (sTime.length == 1) {
                int days = 0, hours = 0, minutes = 0;
                int lastIndex = 0, currIndex = 1;
                while (currIndex <= sTime[0].length()) {
                    while (currIndex <= sTime[0].length() && isInt(sTime[0].substring(lastIndex, currIndex))) {
                        currIndex++;
                    }
                    if (currIndex - 1 != lastIndex) {
                        final String param = sTime[0].substring(currIndex - 1, currIndex).toLowerCase();
                        if (param.equals("d")) {
                            days = Integer.parseInt(sTime[0].substring(lastIndex, currIndex - 1));
                        } else if (param.equals("h")) {
                            hours = Integer.parseInt(sTime[0].substring(lastIndex, currIndex - 1));
                        } else if (param.equals("m")) {
                            minutes = Integer.parseInt(sTime[0].substring(lastIndex, currIndex - 1));
                        }
                    }
                    lastIndex = currIndex;
                    currIndex++;
                }
                if (days == 0 && hours == 0 && minutes == 0) {
                    return -1;
                }
                return minutes + hours * 60 + days * 1440;
            } else {
                return -1;
            }
        }
        final String timestamp;
        if (sTime.length == 1) {
            if (sTime[0].contains(":")) {
                timestamp = new SimpleDateFormat("dd.MM.yyyy").format(System.currentTimeMillis()) + " " + sTime[0];
            } else {
                timestamp = sTime[0] + " 00:00:00";
            }
        } else {
            timestamp = sTime[0] + " " + sTime[1];
        }
        try {
            return (int) ((System.currentTimeMillis() - new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").parse(timestamp).getTime()) / 60000);
        } catch (final ParseException ex) {
            return -1;
        }
    }

    private final static SimpleDateFormat timeFormatter = new SimpleDateFormat("MM-dd HH:mm:ss");

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

    public static Vector toVector(Location location) {
        return new Vector(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static String formatTime(long time) {
        return timeFormatter.format(time);
    }

    public static boolean isInt(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (final NumberFormatException e) {
            return false;
        }
    }

    public static String getPlayerId(String playerName) {
        if (playerName == null) {
            return "NULL";
        }
        final Integer id = BlockActivity.playerIds.get(playerName);
        if (id != null) {
            return id.toString();
        }
        return "(SELECT playerid FROM `ba-players` WHERE playername = '" + playerName + "')";
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

    public static boolean canFall(World world, int x, int y, int z) {
        final Material material = world.getBlockAt(x, y, z).getType();
        return isFallingOverridingBlock(material);
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

    public static boolean isRelativeTopBreakableBlock(Material material) {
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

    public static boolean isRelativeSideBreakableBlock(Material material) {
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

    private static final BlockFace[] relativeSideBlockFaces = new BlockFace[] { BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH,
            BlockFace.UP, BlockFace.DOWN };

    public static List<Block> getSideRelativeBreakableBlocks(Block originBlock) {
        final List<Block> blocks = new ArrayList<Block>();
        for (BlockFace blockFace : relativeSideBlockFaces) {
            Block block = originBlock.getRelative(blockFace);
            if (isRelativeSideBreakableBlock(block.getType())) {
                blocks.add(block);
            }
        }
        return blocks;
    }

    public static final BlockFace[] primaryCardinalDirs = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };

    public static BlockFace yawToFace(float yaw) {
        return primaryCardinalDirs[Math.round(yaw / 90f) & 0x3];
    }

    public static BlockFace turnFace(BlockFace face, boolean right) {
        int dir = face.ordinal();
        if (right) {
            dir++;
            if (dir >= primaryCardinalDirs.length) {
                dir = 0;
            }
        } else {
            dir--;
            if (dir < 0) {
                dir = primaryCardinalDirs.length - 1;
            }
        }
        return primaryCardinalDirs[dir];
    }

}
