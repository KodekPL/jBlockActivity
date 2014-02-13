package jcraft.jblockactivity.session;

import jcraft.jblockactivity.actionlog.ActionLog;

import org.bukkit.Location;

public interface LookupCache {
    public String getMessage();

    public Location getLocation();

    public ActionLog getActionLog();
}
