package jcraft.jblockactivity.listeners;

import static jcraft.jblockactivity.utils.ActivityUtil.getEntityName;
import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.WorldConfig;
import jcraft.jblockactivity.actionlogs.EntityActionLog;
import jcraft.jblockactivity.extradata.EntityExtraData;
import jcraft.jblockactivity.extradata.ExtraData;

import org.bukkit.Location;
import org.bukkit.entity.Damageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CreatureKillListener implements Listener {

    @EventHandler
    public void onCreatureDeath(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Damageable && event.getDamage() >= ((Damageable) event.getEntity()).getHealth()) {
            final WorldConfig config = BlockActivity.getWorldConfig(event.getEntity().getWorld().getName());
            if (config == null || !config.isLogging(LoggingType.creaturekill)) {
                return;
            }

            final int entityType = event.getEntity().getType().getTypeId();
            final Location location = event.getEntity().getLocation();
            final String damagerName = getEntityName(event.getDamager());

            if (!config.loggingCreatures.contains(entityType) || BlockActivity.isHidden(damagerName)) {
                return;
            }

            final ExtraData extraData = EntityExtraData.getExtraData(event.getEntity());
            final EntityActionLog action = new EntityActionLog(LoggingType.creaturekill, damagerName, location.getWorld(), location.toVector(),
                    entityType, 0, extraData);
            BlockActivity.sendActionLog(action);
        }
    }

}
