package jcraft.jblockactivity.listeners;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.actionlog.BlockActionLog;
import jcraft.jblockactivity.config.WorldConfig;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class BlockInteractListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        final WorldConfig config = BlockActivity.getWorldConfig(event.getPlayer().getWorld().getName());
        if (config == null || !config.isLogging(LoggingType.blockinteract)) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        final Player player = event.getPlayer();

        if (BlockActivity.isHidden(player.getUniqueId())) {
            return;
        }

        final Block block = event.getClickedBlock();
        final int typeId = block.getTypeId();
        final byte data = block.getData();
        final Location location = block.getLocation();

        if (config.isInteractiveBlock(typeId)) {
            /** CAKE **/
            if (typeId == 92 && player.getFoodLevel() >= 20) {
                return;
            }

            final BlockActionLog action = new BlockActionLog(LoggingType.blockinteract, player.getName(), location.getWorld(), location.toVector(),
                    typeId, data, typeId, data, null);
            BlockActivity.sendActionLog(action);
        }
    }

}
