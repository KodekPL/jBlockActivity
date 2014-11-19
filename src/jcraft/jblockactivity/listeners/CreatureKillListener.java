package jcraft.jblockactivity.listeners;

import java.util.UUID;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.LoggingMaker;
import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.actionlog.EntityActionLog;
import jcraft.jblockactivity.config.WorldConfig;
import jcraft.jblockactivity.extradata.EntityExtraData;
import jcraft.jblockactivity.extradata.ExtraData;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class CreatureKillListener implements Listener {

    @EventHandler
    public void onCreatureDeath(EntityDeathEvent event) {
        if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            final WorldConfig config = BlockActivity.getWorldConfig(event.getEntity().getWorld().getName());

            if (config == null || !config.isLogging(LoggingType.creaturekill)) {
                return;
            }

            final int entityType = event.getEntity().getType().getTypeId();

            if (!config.loggingCreatures.contains(entityType)) {
                return;
            }

            final Entity killer = ((EntityDamageByEntityEvent) event.getEntity().getLastDamageCause()).getDamager();
            final String killerName = LoggingMaker.getLoggingMaker(killer);

            UUID killerUUID;
            try {
                killerUUID = UUID.fromString(killerName);
            } catch (IllegalArgumentException e) {
                killerUUID = null;
            }

            final Location location = event.getEntity().getLocation();

            if (BlockActivity.isHidden(killerName)) {
                return;
            }

            final ExtraData extraData = EntityExtraData.getExtraData(event.getEntity());
            final EntityActionLog action = new EntityActionLog(LoggingType.creaturekill, killerName, killerUUID, location.getWorld(),
                    location.toVector(), entityType, 0, extraData);

            BlockActivity.sendActionLog(action);
        }
    }

}
