package jcraft.jblockactivity.tool;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.utils.QueryParams;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.material.MaterialData;

public class LogTool {
    public final ToolBehavior leftClickBehavior, rightClickBehavior;
    public final MaterialData itemMaterial;
    public final QueryParams params;

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
            player.setItemInHand(itemMaterial.toItemStack());
            player.sendMessage(BlockActivity.prefix + ChatColor.GREEN + "Tool was added to your inventory!");
        } else {
            player.sendMessage(BlockActivity.prefix + ChatColor.RED + "You have no empty slot in your inventory!");
        }
    }

    public static enum ToolBehavior {
        TOOL, BLOCK, NONE;
    }
}
