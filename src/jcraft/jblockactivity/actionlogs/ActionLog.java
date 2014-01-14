package jcraft.jblockactivity.actionlogs;

import java.sql.Connection;
import java.sql.SQLException;

import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.extradata.ExtraData;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public abstract class ActionLog {

    private long id, time;
    private final LoggingType type;
    private final String playerName, worldName;
    private final Vector location;
    private final ExtraData extraData;

    protected ActionLog(LoggingType type, String playerName, World world, Vector location, ExtraData extraData) {
        this.time = System.currentTimeMillis() / 1000;
        this.type = type;
        this.playerName = playerName;
        this.worldName = world.getName();
        this.location = location;
        this.extraData = extraData;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public LoggingType getLoggingType() {
        return type;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getWorldName() {
        return worldName;
    }

    public Vector getVector() {
        return location;
    }

    public Location getLocation() {
        if (location == null) {
            return null;
        }
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return location.toLocation(world);
    }

    public ExtraData getExtraData() {
        return extraData;
    }

    public ActionLog getLogInstance() {
        return this;
    }

    public abstract void executeStatements(Connection connection) throws SQLException;

}
