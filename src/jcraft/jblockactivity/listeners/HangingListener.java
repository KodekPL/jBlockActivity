package jcraft.jblockactivity.listeners;

import static jcraft.jblockactivity.utils.ActivityUtil.getEntityName;
import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.WorldConfig;
import jcraft.jblockactivity.actionlogs.EntityActionLog;
import jcraft.jblockactivity.extradata.InventoryExtraData;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

public class HangingListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        final WorldConfig config = BlockActivity.getWorldConfig(event.getEntity().getWorld().getName());
        if (config == null || !config.isLogging(LoggingType.hangingplace)) {
            return;
        }

        if (BlockActivity.isHidden(event.getPlayer().getName())) {
            return;
        }

        if (event.getEntity() instanceof ItemFrame) {
            final ItemFrame frame = (ItemFrame) event.getEntity();
            final int entityType = frame.getType().getTypeId();
            final int face = frame.getFacing().ordinal();
            final Location location = frame.getLocation();
            final String playerName = event.getPlayer().getName();

            final EntityActionLog action = new EntityActionLog(LoggingType.hangingplace, playerName, location.getWorld(), location.toVector(),
                    entityType, face, null);
            BlockActivity.sendActionLog(action);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        // HangingBreakByEntityEvent
        if (event.getCause() == RemoveCause.ENTITY) {
            return;
        }

        final WorldConfig config = BlockActivity.getWorldConfig(event.getEntity().getWorld().getName());
        if (config == null || !config.isLogging(LoggingType.hangingbreak)) {
            return;
        }

        if (event.getEntity() instanceof ItemFrame) {
            final ItemFrame frame = (ItemFrame) event.getEntity();
            final int entityType = frame.getType().getTypeId();
            final int face = frame.getFacing().ordinal();
            final Location location = frame.getLocation();

            final String removername;
            switch (event.getCause()) {
            case EXPLOSION:
            case PHYSICS:
            case OBSTRUCTION:
                removername = "BA_" + event.getCause().name();
                break;
            default:
                removername = "unknown";
                break;
            }

            if (config.isLogging(LoggingType.hanginginteract) && frame.getItem().getType() != Material.AIR) {
                final InventoryExtraData extraData = new InventoryExtraData(new ItemStack[] { frame.getItem() }, false);
                final EntityActionLog action = new EntityActionLog(LoggingType.hanginginteract, removername, location.getWorld(),
                        location.toVector(), entityType, face, extraData);
                BlockActivity.sendActionLog(action);
            }

            final EntityActionLog action = new EntityActionLog(LoggingType.hangingbreak, removername, location.getWorld(), location.toVector(),
                    entityType, face, null);
            BlockActivity.sendActionLog(action);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        final WorldConfig config = BlockActivity.getWorldConfig(event.getEntity().getWorld().getName());
        if (config == null || !config.isLogging(LoggingType.hangingbreak)) {
            return;
        }

        if (event.getEntity() instanceof ItemFrame) {
            final ItemFrame frame = (ItemFrame) event.getEntity();
            final int entityType = frame.getType().getTypeId();
            final int face = frame.getFacing().ordinal();
            final Location location = frame.getLocation();
            final String removername = getEntityName(event.getRemover());

            if (BlockActivity.isHidden(removername)) {
                return;
            }

            if (config.isLogging(LoggingType.hanginginteract) && frame.getItem().getType() != Material.AIR) {
                final InventoryExtraData extraData = new InventoryExtraData(new ItemStack[] { frame.getItem() }, false);
                final EntityActionLog action = new EntityActionLog(LoggingType.hanginginteract, removername, location.getWorld(),
                        location.toVector(), entityType, face, extraData);
                BlockActivity.sendActionLog(action);
            }

            final EntityActionLog action = new EntityActionLog(LoggingType.hangingbreak, removername, location.getWorld(), location.toVector(),
                    entityType, face, null);
            BlockActivity.sendActionLog(action);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteractHanging(PlayerInteractEntityEvent event) {
        final WorldConfig config = BlockActivity.getWorldConfig(event.getRightClicked().getWorld().getName());
        if (config == null || !config.isLogging(LoggingType.hanginginteract)) {
            return;
        }

        if (event.getRightClicked() instanceof ItemFrame) {
            final Player player = event.getPlayer();
            final ItemFrame frame = (ItemFrame) event.getRightClicked();

            if (frame.getItem().getType() == Material.AIR && player.getItemInHand().getType() != Material.AIR) {
                final int entityType = frame.getType().getTypeId();
                final int face = frame.getFacing().ordinal();
                final Location location = frame.getLocation();

                final ItemStack item = player.getItemInHand().clone();
                item.setAmount(1);

                final InventoryExtraData extraData = new InventoryExtraData(new ItemStack[] { item }, false);
                final EntityActionLog action = new EntityActionLog(LoggingType.hanginginteract, player.getName(), location.getWorld(),
                        location.toVector(), entityType, face, extraData);
                BlockActivity.sendActionLog(action);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRemoveItemFromHanging(EntityDamageByEntityEvent event) {
        final WorldConfig config = BlockActivity.getWorldConfig(event.getEntity().getWorld().getName());
        if (config == null || !config.isLogging(LoggingType.hanginginteract)) {
            return;
        }

        if (event.getEntity() instanceof ItemFrame && event.getDamager() != null) {
            final ItemFrame frame = (ItemFrame) event.getEntity();

            if (frame.getItem().getType() != Material.AIR) {
                final String removername = getEntityName(event.getDamager());
                final int entityType = frame.getType().getTypeId();
                final int face = frame.getFacing().ordinal();
                final Location location = frame.getLocation();

                final ItemStack item = frame.getItem().clone();
                item.setAmount(-1);

                final InventoryExtraData extraData = new InventoryExtraData(new ItemStack[] { item }, false);
                final EntityActionLog action = new EntityActionLog(LoggingType.hanginginteract, removername, location.getWorld(),
                        location.toVector(), entityType, face, extraData);
                BlockActivity.sendActionLog(action);
            }
        }
    }
}
