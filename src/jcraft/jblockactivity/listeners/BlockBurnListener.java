package jcraft.jblockactivity.listeners;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.LoggingMaker;
import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.config.WorldConfig;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;

public class BlockBurnListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        final WorldConfig config = BlockActivity.getWorldConfig(event.getBlock().getWorld().getName());

        if (config == null || !config.isLogging(LoggingType.blockburn)) {
            return;
        }

        final Block block = event.getBlock();
        final Material material = block.getType();

        if (BlockActivity.isHidden(LoggingMaker.FIRE.getName())) {
            return;
        }

        BlockBreakListener.blockBreak(config, block, material, LoggingMaker.FIRE.getName(), null);
    }

}
