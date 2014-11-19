package jcraft.jblockactivity.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class InventoryUtil {

    public static ItemStack[] packEntityEquipment(EntityEquipment eq) {
        final ItemStack[] eqArray = new ItemStack[5];

        if (isItem(eq.getItemInHand())) {
            eqArray[0] = eq.getItemInHand();
        }

        if (isItem(eq.getHelmet())) {
            eqArray[1] = eq.getHelmet();
        }

        if (isItem(eq.getChestplate())) {
            eqArray[2] = eq.getChestplate();
        }

        if (isItem(eq.getLeggings())) {
            eqArray[3] = eq.getLeggings();
        }

        if (isItem(eq.getBoots())) {
            eqArray[4] = eq.getBoots();
        }

        for (ItemStack item : eqArray) {
            if (item != null) {
                return eqArray;
            }
        }

        return null;
    }

    public static Float[] packEntityEquipmentDropChance(EntityEquipment eq) {
        final Float[] dropChance = new Float[5];

        if (isItem(eq.getItemInHand())) {
            dropChance[0] = eq.getItemInHandDropChance();
        }

        if (isItem(eq.getHelmet())) {
            dropChance[1] = eq.getHelmetDropChance();
        }

        if (isItem(eq.getChestplate())) {
            dropChance[2] = eq.getChestplateDropChance();
        }

        if (isItem(eq.getLeggings())) {
            dropChance[3] = eq.getLeggingsDropChance();
        }

        if (isItem(eq.getBoots())) {
            dropChance[4] = eq.getBootsDropChance();
        }

        return dropChance;
    }

    private static boolean isItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        return true;
    }

    public static boolean isItemSimilar(ItemStack item1, ItemStack item2) {
        return item1.getType() == item2.getType() && item2.getDurability() == item2.getDurability()
                && item1.getItemMeta().hashCode() == item2.getItemMeta().hashCode();
    }

    public static boolean isSameLocation(Vector v1, Vector v2) {
        return v1.getBlockX() == v2.getBlockX() && v1.getBlockY() == v2.getBlockY() && v1.getBlockZ() == v2.getBlockZ();
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
        } else if (holder instanceof Entity) {
            return ((Entity) holder).getLocation();
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

}
