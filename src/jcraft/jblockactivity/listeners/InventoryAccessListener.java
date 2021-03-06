package jcraft.jblockactivity.listeners;

import java.util.HashMap;
import java.util.Map;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.actionlog.BlockActionLog;
import jcraft.jblockactivity.config.WorldConfig;
import jcraft.jblockactivity.extradata.InventoryExtraData;
import jcraft.jblockactivity.utils.InventoryUtil;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;

public class InventoryAccessListener implements Listener {

    private final Map<HumanEntity, InventoryExtraData> invExtraData = new HashMap<HumanEntity, InventoryExtraData>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        final WorldConfig config = BlockActivity.getWorldConfig(event.getPlayer().getWorld().getName());

        if (config == null || !config.isLogging(LoggingType.inventoryaccess)) {
            return;
        }

        if (BlockActivity.isHidden(event.getPlayer().getUniqueId())) {
            return;
        }

        final InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof BlockState || holder instanceof DoubleChest) {
            final HumanEntity player = event.getPlayer();
            final InventoryExtraData lastContent = invExtraData.get(player);

            if (lastContent != null) {
                final Location location = InventoryUtil.getInventoryHolderLocation(holder);
                final InventoryExtraData newContent = new InventoryExtraData(event.getInventory().getContents(), true, location.getWorld());

                lastContent.compareInventories(newContent);

                if (!lastContent.isEmpty()) {
                    final BlockActionLog action = new BlockActionLog(LoggingType.inventoryaccess, player.getName(), player.getUniqueId(),
                            location.getWorld(), location.toVector(), location.getBlock().getState(), location.getBlock().getState(), lastContent);

                    BlockActivity.sendActionLog(action);
                }

                invExtraData.remove(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        final WorldConfig config = BlockActivity.getWorldConfig(event.getPlayer().getWorld().getName());

        if (config == null || !config.isLogging(LoggingType.inventoryaccess)) {
            return;
        }

        if (BlockActivity.isHidden(event.getPlayer().getUniqueId())) {
            return;
        }

        if (event.getInventory() != null) {
            final InventoryHolder holder = event.getInventory().getHolder();

            if (holder instanceof BlockState || holder instanceof DoubleChest) {
                if (InventoryUtil.getInventoryHolderType(holder) != Material.WORKBENCH) {
                    invExtraData.put(event.getPlayer(),
                            new InventoryExtraData(event.getInventory().getContents(), true, event.getPlayer().getWorld()));
                }
            }
        }
    }

}
