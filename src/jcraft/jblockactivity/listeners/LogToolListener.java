package jcraft.jblockactivity.listeners;

import jcraft.jblockactivity.ActionExecuteThread.ActionRequest;
import jcraft.jblockactivity.ActionExecuteThread.ActionRequest.ActionType;
import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.tool.LogTool;
import jcraft.jblockactivity.tool.LogTool.ToolBehavior;
import jcraft.jblockactivity.utils.QueryParams;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.MaterialData;

public class LogToolListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onToolInteract(PlayerInteractEvent event) {
        if (event.getItem() == null) {
            return;
        }

        final Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        final MaterialData itemMaterial = event.getItem().getData();
        final LogTool tool = BlockActivity.getLogItem(itemMaterial);
        final Player player = event.getPlayer();

        if (tool == null || !player.hasPermission("ba.tool.lookup")) {
            return;
        }

        final ToolBehavior behavior = (action == Action.RIGHT_CLICK_BLOCK) ? tool.getRightClickBehavior() : tool.getLeftClickBehavior();
        if (behavior == ToolBehavior.NONE) {
            return;
        }

        if (!player.hasPermission("ba.tool.nocooldown")) {
            if (BlockActivity.lastToolUse.containsKey(player.getName())) {
                if (System.currentTimeMillis() - BlockActivity.lastToolUse.get(player.getName()) < BlockActivity.config.toolUseCooldown) {
                    player.sendMessage(BlockActivity.prefix + ChatColor.RED + "You can't use lookup tool that often!");
                    event.setCancelled(true);
                    return;
                }
            }
            BlockActivity.lastToolUse.put(player.getName(), System.currentTimeMillis());
        }

        if (!BlockActivity.isWorldLogged(player.getWorld().getName())) {
            player.sendMessage(BlockActivity.prefix + ChatColor.RED + "This world is not currently logged.");
            event.setCancelled(true);
            return;
        }

        final Block block = event.getClickedBlock();
        final QueryParams params = tool.getParams();

        if (behavior == ToolBehavior.BLOCK) {
            params.setLocation(block.getRelative(event.getBlockFace()).getLocation());
        } else {
            params.setLocation(block.getLocation());
        }
        BlockActivity.getCommandHandler().preExecuteCommand(new ActionRequest(ActionType.CMD_LOOKUP, player, params, false), true);
        event.setCancelled(true);
    }

    @EventHandler
    public void onDropItem(ItemSpawnEvent event) {
        final MaterialData itemMaterial = event.getEntity().getItemStack().getData();
        final LogTool tool = BlockActivity.getLogItem(itemMaterial);
        if (tool != null) {
            event.getEntity().remove();
        }
    }

}
