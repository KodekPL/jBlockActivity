package jcraft.jblockactivity;

import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.projectiles.ProjectileSource;

public enum LoggingMaker {

    FIRE("FIRE"), UNKNOWN("UNKNOWN"), EXPLOSION("EXPLOSION"), PHYSICS("PHYSICS"), OBSTRUCTION("OBSTRUCTION");

    private final String name;

    LoggingMaker(String name) {
        this.name = "BA_" + name.toUpperCase();
    }

    public String getName() {
        return name;
    }

    public static LoggingMaker getLoggingMaker(org.bukkit.event.hanging.HangingBreakEvent.RemoveCause cause) {
        switch (cause) {
        case EXPLOSION:
            return LoggingMaker.EXPLOSION;
        case PHYSICS:
            return LoggingMaker.PHYSICS;
        case OBSTRUCTION:
            return LoggingMaker.OBSTRUCTION;
        default:
            return LoggingMaker.UNKNOWN;
        }
    }

    public static String getLoggingMaker(Entity entity) {
        return getLoggingMaker(entity, false);
    }

    public static String getLoggingMaker(Entity entity, boolean explosion) {
        if (entity == null) {
            return LoggingMaker.UNKNOWN.getName();
        }

        final String name;

        if (entity instanceof Player) {
            name = ((Player) entity).getUniqueId().toString();
        } else if (entity instanceof Projectile && ((Projectile) entity).getShooter() != null) {
            final ProjectileSource shooter = ((Projectile) entity).getShooter();

            if (shooter instanceof Player) {
                name = ((Player) shooter).getUniqueId().toString();
            } else {
                name = "BA_" + ((Entity) shooter).getType().name().replace('_', ' ').toUpperCase();
            }
        } else if (entity instanceof TNTPrimed && ((TNTPrimed) entity).getSource() != null) {
            final Entity source = ((TNTPrimed) entity).getSource();

            if (source instanceof Player) {
                name = ((Player) source).getUniqueId().toString();
            } else if (source instanceof Projectile && ((Projectile) entity).getShooter() != null) {
                final ProjectileSource shooter = ((Projectile) source).getShooter();

                if (shooter instanceof Player) {
                    name = ((Player) shooter).getUniqueId().toString();
                } else {
                    name = "BA_" + ((Entity) shooter).getType().name().replace('_', ' ').toUpperCase();
                }
            } else {
                name = "BA_" + source.getType().name().replace('_', ' ').toUpperCase();
            }
        } else if (entity instanceof Creeper && explosion && ((Creeper) entity).getTarget() != null) {
            final LivingEntity target = ((Creeper) entity).getTarget();

            if (target instanceof Player) {
                name = ((Player) target).getUniqueId().toString();
            } else {
                name = "BA_" + entity.getType().name().replace('_', ' ').toUpperCase();
            }
        } else {
            name = "BA_" + entity.getType().name().replace('_', ' ').toUpperCase();
        }
        return name;
    }

}
