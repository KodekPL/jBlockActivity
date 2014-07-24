package jcraft.jblockactivity.listeners;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.LoggingMaker;
import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.actionlog.BlockActionLog;
import jcraft.jblockactivity.config.WorldConfig;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
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

            final BlockActionLog action = new BlockActionLog(LoggingType.blockinteract, player.getName(), player.getUniqueId(), location.getWorld(),
                    location.toVector(), typeId, data, typeId, data, null);
            BlockActivity.sendActionLog(action);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTrampleFarmland(PlayerInteractEvent event) {
        final WorldConfig config = BlockActivity.getWorldConfig(event.getPlayer().getWorld().getName());
        if (config == null || !config.isLogging(LoggingType.tramplefarmland)) {
            return;
        }

        if (event.getAction() != Action.PHYSICAL) {
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

        if (typeId == 60) {
            final BlockActionLog action = new BlockActionLog(LoggingType.tramplefarmland, player.getName(), player.getUniqueId(),
                    location.getWorld(), location.toVector(), typeId, data, 3, (byte) 0, null);
            BlockActivity.sendActionLog(action);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityTrampleFarmland(EntityInteractEvent event) {
        final WorldConfig config = BlockActivity.getWorldConfig(event.getEntity().getWorld().getName());
        if (config == null || !config.isLogging(LoggingType.tramplefarmland)) {
            return;
        }

        final Entity entity = event.getEntity();

        if (BlockActivity.isHidden(entity.getUniqueId())) {
            return;
        }

        final Block block = event.getBlock();
        final int typeId = block.getTypeId();
        final byte data = block.getData();
        final Location location = block.getLocation();
        final String entityName = LoggingMaker.getLoggingMaker(entity);

        if (typeId == 60) {
            final BlockActionLog action = new BlockActionLog(LoggingType.tramplefarmland, entityName, entity.getUniqueId(), location.getWorld(),
                    location.toVector(), typeId, data, 3, (byte) 0, null);
            BlockActivity.sendActionLog(action);
        }
    }

}
