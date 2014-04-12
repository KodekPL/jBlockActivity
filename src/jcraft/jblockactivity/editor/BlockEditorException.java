package jcraft.jblockactivity.editor;

import jcraft.jblockactivity.actionlog.ActionLog;
import jcraft.jblockactivity.session.LookupCache;
import jcraft.jblockactivity.utils.MaterialNames;

import org.bukkit.Location;

public class BlockEditorException extends Exception implements LookupCache {

    private static final long serialVersionUID = 2182077864822995737L;
    private final Location location;

    public BlockEditorException(int typeBefore, byte dataBefore, int typeAfter, byte dataAfter, Location location) {
        this("Failed to replace " + MaterialNames.materialName(typeBefore, dataBefore) + " with " + MaterialNames.materialName(typeAfter, dataAfter),
                location);
    }

    public BlockEditorException(String msg, Location location) {
        super(msg + " at " + location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ());
        this.location = location;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public ActionLog getActionLog() {
        return null;
    }

}