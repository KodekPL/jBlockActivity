package jcraft.jblockactivity.actionlog;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.extradata.ExtraData;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

public abstract class ActionLog {

    private long time;
    private LoggingType type;
    private UUID uuid;
    private String playerName, worldName;
    private Vector location;
    private ExtraData extraData;

    protected ActionLog(LoggingType type, String playerName, World world, Vector location, ExtraData extraData) {
        this(type, playerName, null, world, location, extraData);
    }

    protected ActionLog(LoggingType type, String playerName, UUID uuid, World world, Vector location, ExtraData extraData) {
        this.time = System.currentTimeMillis() / 1000;
        this.type = type;
        this.playerName = playerName;
        this.uuid = uuid;
        this.worldName = world.getName();
        this.location = location;
        this.extraData = extraData;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public void setLoggingType(LoggingType type) {
        this.type = type;
    }

    public LoggingType getLoggingType() {
        return type;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getIdentifier() {
        if (uuid == null) {
            return playerName;
        }
        return uuid.toString().replace("-", "");
    }

    public String getColoredPlayerName() {
        if (playerName.startsWith("BA_")) {
            return playerName.replace("BA_", ChatColor.GREEN.toString());
        }

        return ChatColor.GOLD + getPlayerName();
    }

    public String getWorldTableName() {
        return BlockActivity.getWorldTableName(worldName);
    }

    public void setVector(BlockVector vector) {
        this.location = vector;
    }

    public void setLocation(Location location) {
        this.location = location.toVector();
    }

    public Vector getVector() {
        return location;
    }

    public void setWorld(World world) {
        this.worldName = world.getName();
    }

    public World getWorld() {
        final World world = Bukkit.getWorld(worldName);
        return world;
    }

    public Location getLocation() {
        if (location == null) {
            return null;
        }

        final World world = getWorld();

        if (world == null) {
            return null;
        }

        return location.toLocation(world);
    }

    public void setExtraData(ExtraData extraData) {
        this.extraData = extraData;
    }

    public ExtraData getExtraData() {
        return extraData;
    }

    public ActionLog getLogInstance() {
        return this;
    }

    public abstract void executeStatements(Connection connection) throws SQLException;

    public abstract String getMessage();

}
