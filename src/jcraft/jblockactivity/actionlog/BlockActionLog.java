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
import jcraft.jblockactivity.extradata.BlockExtraData;
import jcraft.jblockactivity.extradata.BlockExtraData.CommandBlockExtraData;
import jcraft.jblockactivity.extradata.BlockExtraData.SignExtraData;
import jcraft.jblockactivity.extradata.BlockExtraData.SkullExtraData;
import jcraft.jblockactivity.extradata.ExtraData;
import jcraft.jblockactivity.extradata.InventoryExtraData;
import jcraft.jblockactivity.session.LookupCache;
import jcraft.jblockactivity.utils.MaterialNames;
import jcraft.jblockactivity.utils.QueryParams;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class BlockActionLog extends ActionLog implements LookupCache {

    private int newBlockId, oldBlockId;
    private byte newBlockData, oldBlockData;

    public BlockActionLog(LoggingType type, String playerName, World world, Vector location, BlockState oldState, BlockState newState,
            ExtraData extraData) {
        super(type, playerName, world, location, extraData);
        if (oldState != null) {
            this.oldBlockId = oldState.getTypeId();
            this.oldBlockData = oldState.getRawData();
        }
        if (newState != null) {
            this.newBlockId = newState.getTypeId();
            this.newBlockData = newState.getRawData();
        }
    }

    public BlockActionLog(LoggingType type, String playerName, World world, Vector location, int oldBlockId, byte oldBlockData, int newBlockId,
            byte newBlockData, ExtraData extraData) {
        super(type, playerName, world, location, extraData);
        this.oldBlockId = oldBlockId;
        this.oldBlockData = oldBlockData;
        this.newBlockId = newBlockId;
        this.newBlockData = newBlockData;
    }

    public int getNewBlockId() {
        return newBlockId;
    }

    public byte getNewBlockData() {
        return newBlockData;
    }

    public int getOldBlockId() {
        return oldBlockId;
    }

    public byte getOldBlockData() {
        return oldBlockData;
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
            state.setInt(3, getOldBlockId());
            state.setInt(4, getOldBlockData());
            state.setInt(5, getNewBlockId());
            state.setInt(6, getNewBlockData());
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
        final String interact = ChatColor.YELLOW + "> " + ChatColor.GOLD + getPlayerName() + " " + ChatColor.WHITE;

        final StringBuilder msg = new StringBuilder();

        if (getLoggingType() == LoggingType.blockplace || getLoggingType() == LoggingType.blockbreak) {
            msg.append(ChatColor.GRAY).append(formatTime(getTime())).append(' ');
            if (getLoggingType() == LoggingType.blockplace) {
                if (oldBlockId == 0) {
                    msg.append(add).append("created ").append(MaterialNames.materialName(newBlockId, newBlockData));
                } else {
                    msg.append(sub).append("replaced ").append(MaterialNames.materialName(oldBlockId, oldBlockData)).append(" with ")
                            .append(MaterialNames.materialName(newBlockId, newBlockData));
                }
            } else if (getLoggingType() == LoggingType.blockbreak) {
                msg.append(sub).append("destroyed ").append(MaterialNames.materialName(oldBlockId, oldBlockData));
            }

            if (getExtraData() != null) {
                final ExtraData extraData = getExtraData();
                if (extraData instanceof BlockExtraData) {
                    if (newBlockId == 63 || newBlockId == 68 || oldBlockId == 63 || oldBlockId == 68) {
                        final BlockExtraData.SignExtraData data = (SignExtraData) extraData;
                        msg.append(ChatColor.GRAY);
                        for (int i = 0; i < 4; i++) {
                            msg.append(" [").append(ChatColor.stripColor(data.getText()[i])).append(']');
                        }
                    } else if (newBlockId == 144 || oldBlockId == 144) {
                        final BlockExtraData.SkullExtraData data = (SkullExtraData) extraData;
                        if (!data.getName().equals("")) {
                            msg.append(ChatColor.GRAY).append(" [").append(data.getName()).append(']');
                        }
                    } else if (oldBlockId == 137) {
                        final BlockExtraData.CommandBlockExtraData data = (CommandBlockExtraData) extraData;
                        if (!data.getCommand().equals("")) {
                            String cmd = data.getCommand();
                            if (cmd.length() > 16) {
                                cmd = data.getCommand().substring(0, 16) + "...";
                            }
                            msg.append(ChatColor.GRAY).append(" [").append(cmd).append(']');
                        }
                    }
                }
            }

            if (getVector() != null) {
                msg.append(" at ").append(getVector().getBlockX()).append(':').append(getVector().getBlockY()).append(':')
                        .append(getVector().getBlockZ());
            }
            msg.append(ChatColor.GRAY).append(" (").append(getTimeSince()).append(')');
        } else if (getLoggingType() == LoggingType.inventoryaccess && getExtraData() != null) {
            final String prefixTime = ChatColor.GRAY + formatTime(getTime()) + " ";
            final String suffixTime = ChatColor.GRAY + " (" + getTimeSince() + ")";
            if (getExtraData() instanceof InventoryExtraData) {
                final InventoryExtraData extraData = (InventoryExtraData) getExtraData();
                for (int i = 0; i < extraData.getContent().length; i++) {
                    ItemStack item = extraData.getContent()[i];
                    if (item == null) continue;
                    if (item.getAmount() < 0) {
                        msg.append(prefixTime).append(sub).append("took ").append(-item.getAmount()).append("x ")
                                .append(MaterialNames.materialName(item.getTypeId(), item.getData().getData()));
                        if (item.hasItemMeta()) msg.append(" [e]");
                        msg.append(" from ").append(MaterialNames.materialName(newBlockId, newBlockData)).append(suffixTime);
                    } else {
                        msg.append(prefixTime).append(add).append("put ").append(item.getAmount()).append("x ")
                                .append(MaterialNames.materialName(item.getTypeId(), item.getData().getData()));
                        if (item.hasItemMeta()) msg.append(" [e]");
                        msg.append(" into ").append(MaterialNames.materialName(newBlockId, newBlockData)).append(suffixTime);
                    }
                    if (i < extraData.getContent().length) {
                        msg.append('\n');
                    }
                }
            }
        } else if (getLoggingType() == LoggingType.blockinteract) {
            msg.append(ChatColor.GRAY).append(formatTime(getTime())).append(' ');
            msg.append(interact).append("interact with ").append(MaterialNames.materialName(newBlockId, newBlockData));

            if (getVector() != null) {
                msg.append(" at ").append(getVector().getBlockX()).append(':').append(getVector().getBlockY()).append(':')
                        .append(getVector().getBlockZ());
            }
            msg.append(ChatColor.GRAY).append(" (").append(getTimeSince()).append(')');
        }

        return msg.toString();
    }

    @Override
    public ActionLog getActionLog() {
        return this;
    }

    public static BlockActionLog getBlockActionLog(ResultSet result, QueryParams params) throws SQLException {
        final int id = params.needId ? result.getInt("id") : 0;
        final long time = params.needTime ? result.getTimestamp("time").getTime() : 0;
        final LoggingType logType = params.needLogType ? LoggingType.getTypeById(result.getInt("type")) : null;
        final Vector location = params.needCoords ? new Vector(result.getInt("x"), result.getInt("y"), result.getInt("z")) : null;
        final String playerName = params.needPlayer ? result.getString("playername") : " ";
        final int old_id = params.needMaterial ? result.getInt("old_id") : 0;
        final int new_id = params.needMaterial ? result.getInt("new_id") : 0;
        final byte old_data = params.needData ? result.getByte("old_data") : (byte) 0;
        final byte new_data = params.needData ? result.getByte("new_data") : (byte) 0;
        final String data = params.needExtraData ? result.getString("data") : null;
        ExtraData extraData = null;
        if (data != null) {
            if (logType == LoggingType.blockbreak || logType == LoggingType.blockplace) {
                if (old_id == 63 || old_id == 68 || new_id == 63 || new_id == 68) {
                    extraData = fromJson(data, BlockExtraData.SignExtraData.class);
                } else if (old_id == 144 || new_id == 144) {
                    extraData = fromJson(data, BlockExtraData.SkullExtraData.class);
                } else if (old_id == 52) {
                    extraData = fromJson(data, BlockExtraData.MobSpawnerExtraData.class);
                } else if (old_id == 140) {
                    // TODO: Flower pot contents
                } else if (old_id == 137) {
                    extraData = fromJson(data, BlockExtraData.CommandBlockExtraData.class);
                }
            } else if (logType == LoggingType.inventoryaccess) {
                extraData = fromJson(data, InventoryExtraData.class);
            }
        }
        final BlockActionLog log = new BlockActionLog(logType, playerName, params.getWorld(), location, old_id, old_data, new_id, new_data, extraData);
        log.setId(id);
        log.setTime(time);
        return log;
    }

}
