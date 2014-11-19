package jcraft.jblockactivity.listeners;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.actionlog.BlockActionLog;
import jcraft.jblockactivity.config.WorldConfig;

import org.bukkit.Material;
import org.bukkit.block.Block;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.cache.LastAccessExtentCache;
import com.sk89q.worldedit.extent.logging.AbstractLoggingExtent;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.World;

public class WorldEditListener {

    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        if (event.getExtent() instanceof LastAccessExtentCache) {
            final Actor actor = event.getActor();
            final World world = event.getWorld();

            if (actor != null && actor.isPlayer() && world != null && world instanceof BukkitWorld) {
                final WorldConfig config = BlockActivity.getWorldConfig(world.getName());

                if (config != null && config.isLogging(LoggingType.worldedit)) {
                    event.setExtent(new WorldEditChangeLogger(actor, (BukkitWorld) event.getWorld(), config, event.getExtent()));
                }
            }
        }
    }

    static class WorldEditChangeLogger extends AbstractLoggingExtent {

        private final Actor actor;
        private final BukkitWorld world;
        private final WorldConfig config;

        protected WorldEditChangeLogger(Actor actor, BukkitWorld world, WorldConfig config, Extent extent) {
            super(extent);
            this.actor = actor;
            this.world = world;
            this.config = config;
        }

        @Override
        protected void onBlockChange(Vector position, BaseBlock newBlock) {
            final Block oldBlock = world.getWorld().getBlockAt(position.getBlockX(), position.getBlockY(), position.getBlockZ());

            if (oldBlock.getType() == Material.AIR) {
                final BlockActionLog action = new BlockActionLog(LoggingType.blockplace, actor.getName(), actor.getUniqueId(), oldBlock.getWorld(),
                        oldBlock.getLocation().toVector(), 0, (byte) 0, newBlock.getId(), (byte) newBlock.getData(), null);

                BlockActivity.sendActionLog(action);
            } else {
                if (newBlock.getId() == 0) {
                    BlockBreakListener.blockBreak(config, oldBlock, oldBlock.getType(), actor.getName(), actor.getUniqueId(), true);
                } else {
                    final BlockActionLog action = new BlockActionLog(LoggingType.blockplace, actor.getName(), actor.getUniqueId(),
                            oldBlock.getWorld(), oldBlock.getLocation().toVector(), oldBlock.getTypeId(), oldBlock.getData(), newBlock.getId(),
                            (byte) newBlock.getData(), null);

                    BlockActivity.sendActionLog(action);
                }
            }
        }

    }

}
