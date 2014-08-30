package jcraft.jblockactivity.actionlog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.extradata.EntityExtraData;
import jcraft.jblockactivity.extradata.EntityExtraData.PaintingExtraData;
import jcraft.jblockactivity.extradata.ExtraData;
import jcraft.jblockactivity.extradata.InventoryExtraData;
import jcraft.jblockactivity.session.LookupCache;
import jcraft.jblockactivity.utils.ActivityUtil;
import jcraft.jblockactivity.utils.MaterialNames;
import jcraft.jblockactivity.utils.QueryParams;
import jcraft.jblockactivity.utils.QueryParams.ParamType;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class EntityActionLog extends ActionLog implements LookupCache {

    private int entityId, entityData;

    public EntityActionLog(LoggingType type, String playerName, World world, Vector location, int entityId, int dataId, ExtraData extraData) {
        this(type, playerName, null, world, location, entityId, dataId, extraData);
    }

    public EntityActionLog(LoggingType type, String playerName, UUID uuid, World world, Vector location, int entityId, int dataId, ExtraData extraData) {
        super(type, playerName, uuid, world, location, extraData);
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
            state = connection.prepareStatement(
                    "INSERT INTO `" + getWorldTableName()
                            + "` (time, type, playerid, old_id, old_data, new_id, new_data, x, y, z) VALUES (FROM_UNIXTIME(?), ?, "
                            + BlockActivity.getPlayerId(getIdentifier()) + ", ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
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
            msg.append(ChatColor.GRAY).append(ActivityUtil.formatTime(getTime())).append(' ');

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
        } else if (getLoggingType() == LoggingType.hanginginteract && getExtraData() != null && !getExtraData().isNull()) {
            msg.append(ChatColor.GRAY).append(ActivityUtil.formatTime(getTime())).append(' ');
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
            msg.append(ChatColor.GRAY).append(ActivityUtil.formatTime(getTime())).append(' ').append(sub).append("killed ")
                    .append(MaterialNames.entityName(getEntityId()));
            /*
             * Skip showing owner name, plugin is now saving only owner UUID and it is not worth asking database for addition names
             * if (getExtraData() != null) {
             * String ownerName = null;
             * if (getEntityId() == 95) {
             * final WolfExtraData extraData = (WolfExtraData) getExtraData();
             * if (extraData.getOwner() != null) ownerName = extraData.getOwner();
             * } else if (getEntityId() == 98) {
             * final OcelotExtraData extraData = (OcelotExtraData) getExtraData();
             * if (extraData.getOwner() != null) ownerName = extraData.getOwner();
             * } else if (getEntityId() == 100) {
             * final HorseExtraData extraData = (HorseExtraData) getExtraData();
             * if (extraData.getOwner() != null) ownerName = extraData.getOwner();
             * }
             * if (ownerName != null) msg.append(ChatColor.GRAY).append(" [").append(ownerName).append(']');
             * }
             */
            msg.append(" (").append(getTimeSince()).append(')');
        }

        return msg.toString();
    }

    @Override
    public ActionLog getActionLog() {
        return this;
    }

    public static EntityActionLog getEntityActionLog(ResultSet result, QueryParams params) throws SQLException {
        final long time = params.getBoolean(ParamType.NEED_TIME) ? result.getTimestamp("time").getTime() : 0;
        final LoggingType logType = params.getBoolean(ParamType.NEED_LOG_TYPE) ? LoggingType.getTypeById(result.getInt("type")) : null;
        final Vector location = params.getBoolean(ParamType.NEED_COORDS) ? new Vector(result.getInt("x"), result.getInt("y"), result.getInt("z"))
                : null;
        final String playerName = params.getBoolean(ParamType.NEED_PLAYER) ? result.getString("playername") : " ";
        final String sUUID = params.getBoolean(ParamType.NEED_PLAYER) ? result.getString("uuid") : null;
        final String data = params.getBoolean(ParamType.NEED_EXTRA_DATA) ? result.getString("data") : null;

        int entity_id = 0;
        int entity_data = 0;
        if (logType == LoggingType.hangingbreak || logType == LoggingType.creaturekill) {
            entity_id = params.getBoolean(ParamType.NEED_MATERIAL) ? result.getInt("old_id") : 0;
            entity_data = params.getBoolean(ParamType.NEED_DATA) ? result.getByte("old_data") : (byte) 0;
        } else {
            entity_id = params.getBoolean(ParamType.NEED_MATERIAL) ? result.getInt("new_id") : 0;
            entity_data = params.getBoolean(ParamType.NEED_DATA) ? result.getByte("new_data") : (byte) 0;
        }

        ExtraData extraData = null;
        if (data != null) {
            if (logType == LoggingType.hangingplace || logType == LoggingType.hangingbreak) {
                if (entity_id == 9) {
                    extraData = ActivityUtil.fromJson(data, PaintingExtraData.class);
                }
            } else if (logType == LoggingType.hanginginteract) {
                extraData = ActivityUtil.fromJson(data, InventoryExtraData.class);
            } else if (logType == LoggingType.creaturekill) {
                if (entity_id == 50) {
                    extraData = ActivityUtil.fromJson(data, EntityExtraData.CreeperExtraData.class);
                } else if (entity_id == 51) {
                    extraData = ActivityUtil.fromJson(data, EntityExtraData.SkeletonExtraData.class);
                } else if (entity_id == 54) {
                    extraData = ActivityUtil.fromJson(data, EntityExtraData.ZombieExtraData.class);
                } else if (entity_id == 55) {
                    extraData = ActivityUtil.fromJson(data, EntityExtraData.SlimeExtraData.class);
                } else if (entity_id == 58) {
                    extraData = ActivityUtil.fromJson(data, EntityExtraData.EndermanExtraData.class);
                } else if (entity_id == 62) {
                    extraData = ActivityUtil.fromJson(data, EntityExtraData.MagmaCubeExtraData.class);
                } else if (entity_id == 90) {
                    extraData = ActivityUtil.fromJson(data, EntityExtraData.PigExtraData.class);
                } else if (entity_id == 91) {
                    extraData = ActivityUtil.fromJson(data, EntityExtraData.SheepExtraData.class);
                } else if (entity_id == 95) {
                    extraData = ActivityUtil.fromJson(data, EntityExtraData.WolfExtraData.class);
                } else if (entity_id == 98) {
                    extraData = ActivityUtil.fromJson(data, EntityExtraData.OcelotExtraData.class);
                } else if (entity_id == 99) {
                    extraData = ActivityUtil.fromJson(data, EntityExtraData.IronGolemExtraData.class);
                } else if (entity_id == 100) {
                    extraData = ActivityUtil.fromJson(data, EntityExtraData.HorseExtraData.class);
                } else if (entity_id == 120) {
                    extraData = ActivityUtil.fromJson(data, EntityExtraData.VillagerExtraData.class);
                }
            }
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(ActivityUtil.fixUUID(sUUID));
        } catch (IllegalArgumentException e) {
            uuid = null;
        }

        final EntityActionLog log = new EntityActionLog(logType, playerName, uuid, params.getWorld(), location, entity_id, entity_data, extraData);
        log.setTime(time);
        return log;
    }

}
