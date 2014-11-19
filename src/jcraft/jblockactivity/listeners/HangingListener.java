package jcraft.jblockactivity.listeners;

import java.util.UUID;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.LoggingMaker;
import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.actionlog.EntityActionLog;
import jcraft.jblockactivity.config.WorldConfig;
import jcraft.jblockactivity.extradata.EntityExtraData;
import jcraft.jblockactivity.extradata.InventoryExtraData;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
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

        final UUID playerUUID = event.getPlayer().getUniqueId();
        final String playerName = event.getPlayer().getName();
        final int entityType = event.getEntity().getType().getTypeId();

        if (!config.loggingHangings.contains(entityType)) {
            return;
        }

        if (BlockActivity.isHidden(playerUUID)) {
            return;
        }

        if (event.getEntity() instanceof ItemFrame) {
            final ItemFrame frame = (ItemFrame) event.getEntity();
            final int face = frame.getFacing().ordinal();
            final Location location = frame.getLocation();

            final EntityActionLog action = new EntityActionLog(LoggingType.hangingplace, playerName, playerUUID, location.getWorld(),
                    location.toVector(), entityType, face, null);

            BlockActivity.sendActionLog(action);
        } else if (event.getEntity() instanceof Painting) {
            final Painting painting = (Painting) event.getEntity();
            final int face = painting.getFacing().ordinal();
            final Location location = painting.getLocation();

            final EntityActionLog action = new EntityActionLog(LoggingType.hangingplace, playerName, playerUUID, location.getWorld(),
                    location.toVector(), entityType, face, EntityExtraData.getExtraData(painting));

            BlockActivity.sendActionLog(action);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        if (event.getCause() == RemoveCause.ENTITY) {
            // HangingBreakByEntityEvent
            return;
        }

        final WorldConfig config = BlockActivity.getWorldConfig(event.getEntity().getWorld().getName());

        if (config == null || !config.isLogging(LoggingType.hangingbreak)) {
            return;
        }

        final int entityType = event.getEntity().getType().getTypeId();

        if (!config.loggingHangings.contains(entityType)) {
            return;
        }

        final String removerName = LoggingMaker.getLoggingMaker(event.getCause()).getName();

        if (BlockActivity.isHidden(removerName)) {
            return;
        }

        if (event.getEntity() instanceof ItemFrame) {
            final ItemFrame frame = (ItemFrame) event.getEntity();
            final int face = frame.getFacing().ordinal();
            final Location location = frame.getLocation();

            if (config.isLogging(LoggingType.hanginginteract) && frame.getItem().getType() != Material.AIR) {
                final InventoryExtraData extraData = new InventoryExtraData(new ItemStack[] { frame.getItem() }, false, location.getWorld());
                final EntityActionLog action = new EntityActionLog(LoggingType.hanginginteract, removerName, location.getWorld(),
                        location.toVector(), entityType, face, extraData);

                BlockActivity.sendActionLog(action);
            }

            final EntityActionLog action = new EntityActionLog(LoggingType.hangingbreak, removerName, location.getWorld(), location.toVector(),
                    entityType, face, null);

            BlockActivity.sendActionLog(action);
        } else if (event.getEntity() instanceof Painting) {
            final Painting painting = (Painting) event.getEntity();
            final int face = painting.getFacing().ordinal();
            final Location location = painting.getLocation();

            final EntityActionLog action = new EntityActionLog(LoggingType.hangingbreak, removerName, location.getWorld(), location.toVector(),
                    entityType, face, EntityExtraData.getExtraData(painting));

            BlockActivity.sendActionLog(action);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        final WorldConfig config = BlockActivity.getWorldConfig(event.getEntity().getWorld().getName());

        if (config == null || !config.isLogging(LoggingType.hangingbreak)) {
            return;
        }

        final int entityType = event.getEntity().getType().getTypeId();

        if (!config.loggingHangings.contains(entityType)) {
            return;
        }

        final String removerName = LoggingMaker.getLoggingMaker(event.getRemover());

        if (BlockActivity.isHidden(removerName)) {
            return;
        }

        UUID removerUUID;

        try {
            removerUUID = UUID.fromString(removerName);
        } catch (IllegalArgumentException e) {
            removerUUID = null;
        }

        if (event.getEntity() instanceof ItemFrame) {
            final ItemFrame frame = (ItemFrame) event.getEntity();
            final int face = frame.getFacing().ordinal();
            final Location location = frame.getLocation();

            if (config.isLogging(LoggingType.hanginginteract) && frame.getItem().getType() != Material.AIR) {
                final InventoryExtraData extraData = new InventoryExtraData(new ItemStack[] { frame.getItem() }, false, location.getWorld());
                final EntityActionLog action = new EntityActionLog(LoggingType.hanginginteract, removerName, removerUUID, location.getWorld(),
                        location.toVector(), entityType, face, extraData);

                BlockActivity.sendActionLog(action);
            }

            final EntityActionLog action = new EntityActionLog(LoggingType.hangingbreak, removerName, removerUUID, location.getWorld(),
                    location.toVector(), entityType, face, null);

            BlockActivity.sendActionLog(action);
        } else if (event.getEntity() instanceof Painting) {
            final Painting painting = (Painting) event.getEntity();
            final int face = painting.getFacing().ordinal();
            final Location location = painting.getLocation();

            final EntityActionLog action = new EntityActionLog(LoggingType.hangingbreak, removerName, removerUUID, location.getWorld(),
                    location.toVector(), entityType, face, EntityExtraData.getExtraData(painting));

            BlockActivity.sendActionLog(action);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteractHanging(PlayerInteractEntityEvent event) {
        final WorldConfig config = BlockActivity.getWorldConfig(event.getRightClicked().getWorld().getName());

        if (config == null || !config.isLogging(LoggingType.hanginginteract)) {
            return;
        }

        final int entityType = event.getRightClicked().getType().getTypeId();

        if (!config.loggingHangings.contains(entityType)) {
            return;
        }

        if (event.getRightClicked() instanceof ItemFrame) {
            final Player player = event.getPlayer();
            final ItemFrame frame = (ItemFrame) event.getRightClicked();

            if (frame.getItem().getType() == Material.AIR && player.getItemInHand().getType() != Material.AIR) {
                final int face = frame.getFacing().ordinal();
                final Location location = frame.getLocation();

                final ItemStack item = player.getItemInHand().clone();
                item.setAmount(1);

                final InventoryExtraData extraData = new InventoryExtraData(new ItemStack[] { item }, false, location.getWorld());
                final EntityActionLog action = new EntityActionLog(LoggingType.hanginginteract, player.getName(), player.getUniqueId(),
                        location.getWorld(), location.toVector(), entityType, face, extraData);

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

        final int entityType = event.getEntity().getType().getTypeId();

        if (!config.loggingHangings.contains(entityType)) {
            return;
        }

        if (event.getEntity() instanceof ItemFrame && event.getDamager() != null) {
            final ItemFrame frame = (ItemFrame) event.getEntity();

            if (frame.getItem().getType() != Material.AIR) {
                final String removerName = LoggingMaker.getLoggingMaker(event.getDamager());

                UUID removerUUID;

                try {
                    removerUUID = UUID.fromString(removerName);
                } catch (IllegalArgumentException e) {
                    removerUUID = null;
                }

                final int face = frame.getFacing().ordinal();
                final Location location = frame.getLocation();

                final ItemStack item = frame.getItem().clone();
                item.setAmount(-1);

                final InventoryExtraData extraData = new InventoryExtraData(new ItemStack[] { item }, false, location.getWorld());
                final EntityActionLog action = new EntityActionLog(LoggingType.hanginginteract, removerName, removerUUID, location.getWorld(),
                        location.toVector(), entityType, face, extraData);

                BlockActivity.sendActionLog(action);
            }
        }
    }

}
