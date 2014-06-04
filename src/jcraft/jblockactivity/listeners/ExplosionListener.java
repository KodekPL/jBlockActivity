package jcraft.jblockactivity.listeners;

import java.util.UUID;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.LoggingMaker;
import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.actionlog.BlockActionLog;
import jcraft.jblockactivity.config.WorldConfig;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

public class ExplosionListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplosion(EntityExplodeEvent event) {
        final WorldConfig config = BlockActivity.getWorldConfig(event.getLocation().getWorld().getName());
        if (config == null || !config.isLogging(LoggingType.explosions)) {
            return;
        }

        final String destroyerName = LoggingMaker.getLoggingMaker(event.getEntity(), true);
        UUID destroyerUUID;
        try {
            destroyerUUID = UUID.fromString(destroyerName);
        } catch (IllegalArgumentException e) {
            destroyerUUID = null;
        }

        for (Block block : event.blockList()) {
            BlockActivity.sendActionLog(new BlockActionLog(LoggingType.blockbreak, destroyerName, destroyerUUID, block.getWorld(), block
                    .getLocation().toVector(), block.getState(), null, null));
        }
    }

}
