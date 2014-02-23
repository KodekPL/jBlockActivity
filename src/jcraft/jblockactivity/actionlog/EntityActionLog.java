package jcraft.jblockactivity.actionlog;

import static jcraft.jblockactivity.utils.ActivityUtil.formatTime;
import static jcraft.jblockactivity.utils.ActivityUtil.fromJson;
import static jcraft.jblockactivity.utils.ActivityUtil.getPlayerId;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.extradata.EntityExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.HorseExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.OcelotExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.PaintingExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.WolfExtraData;
import jcraft.jblockactivity.extradata.ExtraData;
import jcraft.jblockactivity.extradata.InventoryExtraData;
import jcraft.jblockactivity.session.LookupCache;
import jcraft.jblockactivity.utils.MaterialNames;
import jcraft.jblockactivity.utils.QueryParams;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class EntityActionLog extends ActionLog implements LookupCache {

    private int entityId, entityData;

    public EntityActionLog(LoggingType type, String playerName, World world, Vector location, int entityId, int dataId, ExtraData extraData) {
        super(type, playerName, world, location, extraData);
        this.entityId = entityId;
        this.entityData = dataId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityData(int entityData) {
        this.entityData = entityData;
    }

    public int getEntityData() {
        return entityData;
    }

    @Override
    public void executeStatements(Connection connection) throws SQLException {
        PreparedStatement state = null;
        PreparedStatement extraState = null;
        try {
            state = connection.prepareStatement("INSERT INTO `" + getWorldTableName()
                    + "` (time, type, playerid, old_id, old_data, new_id, new_data, x, y, z) VALUES (FROM_UNIXTIME(?), ?, "
                    + getPlayerId(getPlayerName()) + ", ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            state.setLong(1, getTime());
            state.setInt(2, getLoggingType().getId());
            state.setInt(3, (getLoggingType() == LoggingType.hangingbreak || getLoggingType() == LoggingType.creaturekill) ? getEntityId() : 0);
            state.setInt(4, (getLoggingType() == LoggingType.hangingbreak || getLoggingType() == LoggingType.creaturekill) ? getEntityData() : 0);
            state.setInt(5, (getLoggingType() == LoggingType.hangingplace || getLoggingType() == LoggingType.hanginginteract) ? getEntityId() : 0);
            state.setInt(6, (getLoggingType() == LoggingType.hangingplace || getLoggingType() == LoggingType.hanginginteract) ? getEntityData() : 0);
            state.setInt(7, getVector().getBlockX());
            state.setInt(8, getVector().getBlockY());
            state.setInt(9, getVector().getBlockZ());
            state.executeUpdate();

            final ExtraData extraData = getExtraData();
            final String sData;
            if (extraData != null && (sData = extraData.getData()) != null && !sData.equals("{}")) {
                final int id;
                final ResultSet result = state.getGeneratedKeys();
                result.next();
                id = result.getInt(1);
                extraState = connection.prepareStatement("INSERT INTO `" + getWorldTableName() + "-extra` (id, data) VALUES (?, ?)");
                extraState.setInt(1, id);
                extraState.setString(2, sData);
                extraState.executeUpdate();
            }
        } finally {
            if (state != null) {
                try {
                    state.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (extraState != null) {
                try {
                    extraState.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getTimeSince() {
        final StringBuilder time_ago = new StringBuilder();

        final Date start = new Date(getTime());
        final Date end = new Date();

        final long diffInSeconds = (end.getTime() - start.getTime()) / 1000;

        final long diff[] = { 0L, 0L, 0L, 0L };
        diff[0] = TimeUnit.SECONDS.toDays(diffInSeconds);
        diff[1] = TimeUnit.SECONDS.toHours(diffInSeconds) - (diff[0] * 24);
        diff[2] = TimeUnit.SECONDS.toMinutes(diffInSeconds) - (TimeUnit.SECONDS.toHours(diffInSeconds) * 60);
        diff[3] = TimeUnit.SECONDS.toSeconds(diffInSeconds) - (TimeUnit.SECONDS.toMinutes(diffInSeconds) * 60);

        if (diff[0] >= 1) {
            time_ago.append(diff[0]).append('d');
        }
        if (diff[1] >= 1) {
            time_ago.append(diff[1]).append('h');
        }
        if (diff[2] > 1 && diff[2] < 60) {
            time_ago.append(diff[2]).append('m');
        }
        if (diff[0] == 0 && diff[1] == 0 && diff[2] <= 1) {
            time_ago.append(diff[3]).append('s');
        }
        return time_ago.toString();
    }

    @Override
    public String getMessage() {

        final String add = ChatColor.GREEN + "+ " + getColoredPlayerName() + " " + ChatColor.WHITE;
        final String sub = ChatColor.RED + "- " + getColoredPlayerName() + " " + ChatColor.WHITE;
        final String interact = ChatColor.YELLOW + "> " + getColoredPlayerName() + " " + ChatColor.WHITE;

        final StringBuilder msg = new StringBuilder();

        if (getLoggingType() == LoggingType.hangingbreak || getLoggingType() == LoggingType.hangingplace) {
            msg.append(ChatColor.GRAY).append(formatTime(getTime())).append(' ');

            if (getLoggingType() == LoggingType.hangingplace) {
                msg.append(add).append("created ").append(BlockFace.values()[getEntityData()].name().toLowerCase()).append(' ')
                        .append(MaterialNames.entityName(getEntityId()));
            } else if (getLoggingType() == LoggingType.hangingbreak) {
                msg.append(sub).append("destroyed ").append(BlockFace.values()[getEntityData()].name().toLowerCase()).append(' ')
                        .append(MaterialNames.entityName(getEntityId()));
            }

            if (getVector() != null) {
                msg.append(" at ").append(getVector().getBlockX()).append(':').append(getVector().getBlockY()).append(':')
                        .append(getVector().getBlockZ());
            }
            msg.append(ChatColor.GRAY).append(" (").append(getTimeSince()).append(')');
        } else if (getLoggingType() == LoggingType.hanginginteract && getExtraData() != null) {
            msg.append(ChatColor.GRAY).append(formatTime(getTime())).append(' ');
            if (getExtraData() instanceof InventoryExtraData) {
                final InventoryExtraData extraData = (InventoryExtraData) getExtraData();
                final ItemStack item = (extraData.getContent().length > 0) ? extraData.getContent()[0] : null;
                if (item != null) {
                    if (item.getAmount() < 0) {
                        msg.append(sub).append("took ").append(-item.getAmount()).append("x ")
                                .append(MaterialNames.materialName(item.getTypeId(), item.getData().getData()));
                        if (item.hasItemMeta()) msg.append(" [e]");
                        msg.append(" from item frame");
                    } else {
                        msg.append(add).append("put ").append(item.getAmount()).append("x ")
                                .append(MaterialNames.materialName(item.getTypeId(), item.getData().getData()));
                        if (item.hasItemMeta()) msg.append(" [e]");
                        msg.append(" into item frame");
                    }
                } else {
                    msg.append(interact).append("did something with item frame");
                }
                msg.append(ChatColor.GRAY).append(" (").append(getTimeSince()).append(')');
            }
        } else if (getLoggingType() == LoggingType.creaturekill) {
            msg.append(ChatColor.GRAY).append(formatTime(getTime())).append(' ').append(sub).append("killed ")
                    .append(MaterialNames.entityName(getEntityId()));
            if (getExtraData() != null) {
                String ownerName = null;
                if (getEntityId() == 95) {
                    WolfExtraData extraData = (WolfExtraData) getExtraData();
                    if (extraData.getOwner() != null) ownerName = extraData.getOwner();
                } else if (getEntityId() == 98) {
                    OcelotExtraData extraData = (OcelotExtraData) getExtraData();
                    if (extraData.getOwner() != null) ownerName = extraData.getOwner();
                } else if (getEntityId() == 100) {
                    HorseExtraData extraData = (HorseExtraData) getExtraData();
                    if (extraData.getOwner() != null) ownerName = extraData.getOwner();
                }
                if (ownerName != null) msg.append(ChatColor.GRAY).append(" [").append(ownerName).append(']');
            }
            msg.append(" (").append(getTimeSince()).append(')');
        }

        return msg.toString();
    }

    @Override
    public ActionLog getActionLog() {
        return this;
    }

    public static EntityActionLog getEntityActionLog(ResultSet result, QueryParams params) throws SQLException {
        final int id = params.needId ? result.getInt("id") : 0;
        final long time = params.needTime ? result.getTimestamp("time").getTime() : 0;
        final LoggingType logType = params.needLogType ? LoggingType.getTypeById(result.getInt("type")) : null;
        final Vector location = params.needCoords ? new Vector(result.getInt("x"), result.getInt("y"), result.getInt("z")) : null;
        final String playerName = params.needPlayer ? result.getString("playername") : " ";

        int entity_id = 0;
        int entity_data = 0;
        if (logType == LoggingType.hangingbreak || logType == LoggingType.creaturekill) {
            entity_id = params.needMaterial ? result.getInt("old_id") : 0;
            entity_data = params.needData ? result.getByte("old_data") : (byte) 0;
        } else {
            entity_id = params.needMaterial ? result.getInt("new_id") : 0;
            entity_data = params.needData ? result.getByte("new_data") : (byte) 0;
        }

        final String data = params.needExtraData ? result.getString("data") : null;
        ExtraData extraData = null;
        if (data != null) {
            if (logType == LoggingType.hangingplace || logType == LoggingType.hangingbreak) {
                if (entity_id == 9) {
                    extraData = fromJson(data, PaintingExtraData.class);
                }
            } else if (logType == LoggingType.hanginginteract) {
                extraData = fromJson(data, InventoryExtraData.class);
            } else if (logType == LoggingType.creaturekill) {
                if (entity_id == 50) {
                    extraData = fromJson(data, EntityExtraData.CreeperExtraData.class);
                } else if (entity_id == 51) {
                    extraData = fromJson(data, EntityExtraData.SkeletonExtraData.class);
                } else if (entity_id == 54) {
                    extraData = fromJson(data, EntityExtraData.ZombieExtraData.class);
                } else if (entity_id == 55) {
                    extraData = fromJson(data, EntityExtraData.SlimeExtraData.class);
                } else if (entity_id == 58) {
                    extraData = fromJson(data, EntityExtraData.EndermanExtraData.class);
                } else if (entity_id == 62) {
                    extraData = fromJson(data, EntityExtraData.MagmaCubeExtraData.class);
                } else if (entity_id == 90) {
                    extraData = fromJson(data, EntityExtraData.PigExtraData.class);
                } else if (entity_id == 91) {
                    extraData = fromJson(data, EntityExtraData.SheepExtraData.class);
                } else if (entity_id == 95) {
                    extraData = fromJson(data, EntityExtraData.WolfExtraData.class);
                } else if (entity_id == 98) {
                    extraData = fromJson(data, EntityExtraData.OcelotExtraData.class);
                } else if (entity_id == 99) {
                    extraData = fromJson(data, EntityExtraData.IronGolemExtraData.class);
                } else if (entity_id == 100) {
                    extraData = fromJson(data, EntityExtraData.HorseExtraData.class);
                } else if (entity_id == 120) {
                    extraData = fromJson(data, EntityExtraData.VillagerExtraData.class);
                }
            }
        }
        final EntityActionLog log = new EntityActionLog(logType, playerName, params.getWorld(), location, entity_id, entity_data, extraData);
        log.setId(id);
        log.setTime(time);
        return log;
    }

}
