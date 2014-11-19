package jcraft.jblockactivity.tool;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.utils.QueryParams;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public class LogTool {

    private final ToolBehavior leftClickBehavior, rightClickBehavior;
    private final MaterialData itemMaterial;
    private final QueryParams params;

    public LogTool(ToolBehavior leftClickBehavior, ToolBehavior rightClickBehavior, int itemId, byte itemData, QueryParams params) {
        this.leftClickBehavior = leftClickBehavior;
        this.rightClickBehavior = rightClickBehavior;
        this.itemMaterial = new MaterialData(itemId, itemData);
        this.params = params;
    }

    public void giveTool(Player player) {
        final Inventory inventory = player.getInventory();
        final int freeSlot = inventory.firstEmpty();

        if (freeSlot >= 0) {
            if (player.getItemInHand() != null && player.getItemInHand().getTypeId() != 0) {
                inventory.setItem(freeSlot, player.getItemInHand());
            }

            player.setItemInHand(new ItemStack(itemMaterial.getItemType(), 1, (itemMaterial.getData() > 0) ? itemMaterial.getData() : 0));
            player.sendMessage(BlockActivity.PREFIX + ChatColor.GREEN + "Tool was added to your inventory!");
        } else {
            player.sendMessage(BlockActivity.PREFIX + ChatColor.RED + "You have no empty slot in your inventory!");
        }
    }

    public ToolBehavior getLeftClickBehavior() {
        return leftClickBehavior;
    }

    public ToolBehavior getRightClickBehavior() {
        return rightClickBehavior;
    }

    public MaterialData getItemMaterial() {
        return itemMaterial;
    }

    public QueryParams getParams() {
        return params;
    }

    public static enum ToolBehavior {
        TOOL, BLOCK, NONE;
    }

}
