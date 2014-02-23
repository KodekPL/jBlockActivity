package jcraft.jblockactivity.utils;

import static jcraft.jblockactivity.utils.ActivityUtil.hasExtraData;
import static jcraft.jblockactivity.utils.ActivityUtil.isInt;
import static jcraft.jblockactivity.utils.ActivityUtil.matchEntity;
import static jcraft.jblockactivity.utils.ActivityUtil.parseTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.LoggingType;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;

public class QueryParams {

    private static final Set<String> validParams = new HashSet<String>(Arrays.asList("player", "area", "block", "entity", "sum", "destroyed",
            "breaked", "created", "placed", "kills", "all", "time", "since", "before", "limit", "worldname", "asc", "desc", "coords", "loc",
            "location", "extra"));

    public static enum Order {
        ASC, DESC;
    }

    public static enum SummarizationMode {
        NONE, PLAYERS, BLOCKS, ENTITIES;
    }

    public SummarizationMode mode = SummarizationMode.NONE;
    public LoggingType logType = LoggingType.bothblocks;
    public Order order = Order.DESC;

    private World world;
    public boolean needCount = false, needId = false, needTime = true, needLogType = true, needMaterial = true, needData = true, needPlayer = true,
            needCoords = false, needExtraData = false, prepareToolQuery = false;
    public int limit = -1, before = 0, since = 0, radius = -1;
    public boolean excludePlayersMode = false;
    public final List<String> players = new ArrayList<String>();
    private final List<MaterialData> itemTypes = new ArrayList<MaterialData>();
    private final List<EntityType> entityTypes = new ArrayList<EntityType>();
    private Location location = null;

    public QueryParams(String args) throws IllegalArgumentException {
        this(null, args.trim().split(" "), false);
    }

    public QueryParams(CommandSender sender, String[] args, boolean prepareToolQuery) throws IllegalArgumentException {
        final List<String> argsList = new ArrayList<String>(Arrays.asList(args));
        if (!isValidParam(argsList.get(0))) {
            argsList.remove(0);
        }
        this.prepareToolQuery = prepareToolQuery;
        parseArgs(sender, argsList);
    }

    public String getQuery() {
        if (mode == SummarizationMode.NONE) {
            String select = "SELECT ";
            if (needCount) {
                select += "COUNT(*) AS count";
            } else {
                if (needId) select += getTable() + ".id, ";
                if (needTime) select += "time, ";
                if (needLogType) select += "type, ";
                if (needMaterial) select += "old_id, new_id, ";
                if (needData) select += "old_data, new_data, ";
                if (needPlayer) select += "playername, ";
                if (needCoords) select += "x, y, z, ";
                if (needExtraData) select += "data, ";
                select = select.substring(0, select.length() - 2);
            }
            String from = "FROM " + getTable() + " ";
            if (needPlayer || players.size() > 0) {
                from += "INNER JOIN `ba-players` USING (playerid) ";
            }
            if (needExtraData) {
                from += "LEFT JOIN " + getTable("-extra") + " USING (id) ";
            }
            return select + " " + from + getWhere(logType) + "ORDER BY time " + order + ", id " + order + " " + getLimit();
        } else if (mode == SummarizationMode.BLOCKS) {
            return "SELECT new_id, SUM(created) AS created, SUM(destroyed) AS destroyed FROM ((SELECT new_id, count(*) AS created, 0 AS destroyed FROM "
                    + getTable()
                    + " INNER JOIN `ba-players` USING (playerid) "
                    + getWhere(LoggingType.blockplace)
                    + "GROUP BY new_id) UNION (SELECT old_id AS new_id, 0 AS created, count(*) AS destroyed FROM "
                    + getTable()
                    + " INNER JOIN `ba-players` USING (playerid) "
                    + getWhere(LoggingType.blockbreak)
                    + "GROUP BY old_id)) AS t GROUP BY new_id ORDER BY SUM(created) + SUM(destroyed) " + order + " " + getLimit();
        } else if (mode == SummarizationMode.ENTITIES) {
            return "SELECT old_id, SUM(killed) AS killed FROM (SELECT old_id, count(*) AS killed FROM " + getTable()
                    + " INNER JOIN `ba-players` USING (playerid) " + getWhere(LoggingType.creaturekill)
                    + "GROUP BY old_id) AS t GROUP BY old_id ORDER BY SUM(killed) " + order + " " + getLimit();
        } else {
            return "SELECT playername, SUM(created) AS created, SUM(destroyed) AS destroyed FROM ((SELECT playerid, count(*) AS created, 0 AS destroyed FROM "
                    + getTable()
                    + " "
                    + getWhere(LoggingType.blockplace)
                    + "GROUP BY playerid) UNION (SELECT playerid, 0 AS created, count(*) AS destroyed FROM "
                    + getTable()
                    + " "
                    + getWhere(LoggingType.blockbreak)
                    + "GROUP BY playerid)) AS t INNER JOIN `ba-players` USING (playerid) GROUP BY playerid ORDER BY SUM(created) + SUM(destroyed) "
                    + order + " " + getLimit();
        }
    }

    public String getWhere(LoggingType loggingType) {
        final StringBuilder where = new StringBuilder("WHERE ");
        switch (loggingType) {
        case all:
            if (!itemTypes.isEmpty()) {
                where.append('(');
                for (final MaterialData block : itemTypes) {
                    where.append("((new_id = ").append(block.getItemTypeId());
                    if (block.getData() != -1) {
                        where.append(" AND new_data = ").append(block.getData()).append(')');
                    } else {
                        where.append(')');
                    }
                    where.append(" OR (old_id = ").append(block.getItemTypeId());
                    if (block.getData() != -1) {
                        where.append(" AND old_data = ").append(block.getData()).append(')');
                    } else {
                        where.append(')');
                    }
                    where.append(") OR ");
                }
                where.delete(where.length() - 4, where.length());
                where.append(") AND ");
            }
            break;
        case bothblocks:
            if (!itemTypes.isEmpty()) {
                where.append('(');
                for (final MaterialData block : itemTypes) {
                    where.append("((new_id = ").append(block.getItemTypeId());
                    if (block.getData() != -1) {
                        where.append(" AND new_data = ").append(block.getData()).append(')');
                    } else {
                        where.append(')');
                    }
                    where.append(" OR (old_id = ").append(block.getItemTypeId());
                    if (block.getData() != -1) {
                        where.append(" AND old_data = ").append(block.getData()).append(')');
                    } else {
                        where.append(')');
                    }
                    where.append(") OR ");
                }
                where.delete(where.length() - 4, where.length());
                where.append(" AND ").append("(type = ").append(LoggingType.blockplace.getId()).append(" OR type = ")
                        .append(LoggingType.blockbreak.getId()).append(")) AND ");
            } else {
                where.append("(type = ").append(LoggingType.blockplace.getId()).append(" OR type = ").append(LoggingType.blockbreak.getId())
                        .append(") AND ");
            }
            break;
        case blockplace:
            if (!itemTypes.isEmpty()) {
                where.append('(');
                for (final MaterialData block : itemTypes) {
                    where.append("((new_id = ").append(block.getItemTypeId());
                    if (block.getData() != -1) {
                        where.append(") AND new_data = ").append(block.getData());
                    } else {
                        where.append(')');
                    }
                    where.append(") OR ");
                }
                where.delete(where.length() - 4, where.length());
                where.append(" AND ").append("type = ").append(LoggingType.blockplace.getId()).append(") AND ");
            } else {
                where.append("type = ").append(LoggingType.blockplace.getId()).append(" AND ");
            }
            break;
        case blockbreak:
            if (!itemTypes.isEmpty()) {
                where.append('(');
                for (final MaterialData block : itemTypes) {
                    where.append("((old_id = ").append(block.getItemTypeId());
                    if (block.getData() != -1) {
                        where.append(") AND old_data = ").append(block.getData());
                    } else {
                        where.append(')');
                    }
                    where.append(") OR ");
                }
                where.delete(where.length() - 4, where.length());
                where.append(" AND ").append("type = ").append(LoggingType.blockbreak.getId()).append(") AND ");
            } else {
                where.append("type = ").append(LoggingType.blockbreak.getId()).append(" AND ");
            }
            break;
        case creaturekill:
            if (!entityTypes.isEmpty()) {
                where.append('(');
                for (final EntityType entity : entityTypes) {
                    where.append("((old_id = ").append(entity.getTypeId()).append(')').append(") OR ");
                }
                where.delete(where.length() - 4, where.length());
                where.append(" AND ").append("type = ").append(LoggingType.creaturekill.getId()).append(") AND ");
            } else {
                where.append("type = ").append(LoggingType.creaturekill.getId()).append(" AND ");
            }
            break;
        default:
            break;
        }

        if (location != null) {
            if (radius == 0) {
                compileLocationQuery(where, location.getBlockX(), location.getBlockX(), location.getBlockY(), location.getBlockY(),
                        location.getBlockZ(), location.getBlockZ());

            } else if (radius > 0) {
                compileLocationQuery(where, location.getBlockX() - radius + 1, location.getBlockX() + radius - 1, location.getBlockY() - radius + 1,
                        location.getBlockY() + radius - 1, location.getBlockZ() - radius + 1, location.getBlockZ() + radius - 1);
            }
        }

        if (!players.isEmpty() && mode != SummarizationMode.PLAYERS) {
            if (!excludePlayersMode) {
                where.append('(');
                for (final String playerName : players)
                    where.append("playername = '").append(playerName).append("' OR ");
                where.delete(where.length() - 4, where.length());
                where.append(") AND ");
            } else {
                for (final String playerName : players) {
                    where.append("playername != '").append(playerName).append("' AND ");
                }
            }
        }
        if (since > 0) {
            where.append("time > date_sub(now(), INTERVAL ").append(since).append(" MINUTE) AND ");
        }
        if (before > 0) {
            where.append("time < date_sub(now(), INTERVAL ").append(before).append(" MINUTE) AND ");
        }
        if (where.length() > 6) {
            where.delete(where.length() - 4, where.length());
        } else {
            where.delete(0, where.length());
        }
        return where.toString();
    }

    private void compileLocationQuery(StringBuilder where, int blockX, int blockX2, int blockY, int blockY2, int blockZ, int blockZ2) {
        compileLocationQueryPart(where, "x", blockX, blockX2);
        where.append(" AND ");
        compileLocationQueryPart(where, "y", blockY, blockY2);
        where.append(" AND ");
        compileLocationQueryPart(where, "z", blockZ, blockZ2);
        where.append(" AND ");
    }

    private void compileLocationQueryPart(StringBuilder where, String locValue, int loc, int loc2) {
        final int min = Math.min(loc, loc2);
        final int max = Math.max(loc2, loc);

        if (min == max) {
            where.append(locValue).append(" = ").append(min);
        } else if (max - min > 50) {
            where.append(locValue).append(" >= ").append(min).append(" AND ").append(locValue).append(" <= ").append(max);
        } else {
            where.append(locValue).append(" in (");
            for (int c = min; c < max; c++) {
                where.append(c).append(',');
            }
            where.append(max);
            where.append(')');
        }
    }

    public String getLimit() {
        return (limit > 0) ? "LIMIT " + limit : "";
    }

    public String getTable() {
        return "`" + BlockActivity.getWorldTableName(world.getName()) + "`";
    }

    public String getTable(String suffix) {
        return "`" + BlockActivity.getWorldTableName(world.getName()) + suffix + "`";
    }

    public World getWorld() {
        return world;
    }

    public void setLocation(Location location) {
        this.location = location;
        this.world = location.getWorld();
    }

    public void parseArgs(CommandSender sender, List<String> args) throws IllegalArgumentException {
        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("No parameters specified!");
        }
        final Player player = (sender instanceof Player) ? (Player) sender : null;
        if (player != null && world == null) {
            world = player.getWorld();
        }
        for (int i = 0; i < args.size(); i++) {
            final String param = args.get(i).toLowerCase();
            final String[] values = getValues(args, i + 1);
            if (param.equals("player")) {
                if (values.length < 1) {
                    throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
                }
                for (final String playerName : values) {
                    if (playerName.length() > 0) {
                        if (playerName.contains("!")) {
                            excludePlayersMode = true;
                        }
                        if (playerName.contains("\"")) {
                            players.add(playerName.replaceAll("[^a-zA-Z0-9_]", ""));
                        } else {
                            final List<Player> matches = sender.getServer().matchPlayer(playerName);
                            if (matches.size() > 1) {
                                throw new IllegalArgumentException("Inaccurate playername '" + param + "'");
                            }
                            players.add((matches.size() == 1) ? matches.get(0).getName() : playerName.replaceAll("[^a-zA-Z0-9_]", ""));
                        }
                    }
                }
            } else if (param.equals("block") || param.equals("type")) {
                if (values.length < 1) {
                    throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
                }
                for (final String blockName : values) {
                    if (blockName.contains(":")) {
                        String[] blockNameSplit = blockName.split(":");
                        if (blockNameSplit.length > 2) {
                            throw new IllegalArgumentException("No material matching: '" + blockName + "'");
                        }
                        final int data;
                        try {
                            data = Integer.parseInt(blockNameSplit[1]);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Data type not a valid number: '" + blockNameSplit[1] + "'");
                        }
                        if (data > 255 || data < 0) {
                            throw new IllegalArgumentException("Data type out of range (0-255): '" + data + "'");
                        }
                        final Material mat = Material.matchMaterial(blockNameSplit[0]);
                        if (mat == null) {
                            throw new IllegalArgumentException("No material matching: '" + blockName + "'");
                        }
                        if (hasExtraData(mat)) {
                            needExtraData = true;
                        }
                        itemTypes.add(new MaterialData(mat.getId(), (byte) data));
                    } else {
                        final Material mat = Material.matchMaterial(blockName);
                        if (mat == null) {
                            throw new IllegalArgumentException("No material matching: '" + blockName + "'");
                        }
                        if (hasExtraData(mat)) {
                            needExtraData = true;
                        }
                        itemTypes.add(new MaterialData(mat.getId(), (byte) -1));
                    }
                }
            } else if (param.equals("entity")) {
                if (values.length < 1) {
                    throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
                }
                for (final String entityName : values) {
                    final EntityType ent = matchEntity(entityName);
                    if (ent == null) {
                        throw new IllegalArgumentException("No entity matching: '" + entityName + "'");
                    }
                    needExtraData = true;
                    entityTypes.add(ent);
                }
            } else if (param.equals("area")) {
                if (player == null && !prepareToolQuery) {
                    throw new IllegalArgumentException("You have to ba a player to use area!");
                }
                if (values.length == 0) {
                    radius = BlockActivity.config.defaultDistance;
                    if (!prepareToolQuery) location = player.getLocation();
                } else {
                    if (!isInt(values[0])) {
                        throw new IllegalArgumentException("Not a number: '" + values[0] + "'");
                    }
                    radius = Integer.parseInt(values[0]);
                    if (!prepareToolQuery) location = player.getLocation();
                }
            } else if (param.equals("time") || param.equals("since")) {
                since = (values.length > 0) ? parseTime(values) : BlockActivity.config.defaultTime;
                if (since == -1) {
                    throw new IllegalArgumentException("Failed to parse time spec for '" + param + "'");
                }
            } else if (param.equals("before")) {
                before = (values.length > 0) ? parseTime(values) : BlockActivity.config.defaultTime;
                if (before == -1) {
                    throw new IllegalArgumentException("Faile to parse time spec for '" + param + "'");
                }
            } else if (param.equals("sum")) {
                if (values.length != 1) {
                    throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
                }
                if (values[0].charAt(0) == 'p') {
                    mode = SummarizationMode.PLAYERS;
                    needTime = false;
                    needLogType = false;
                    needMaterial = false;
                    needData = false;
                    needPlayer = false;
                    needExtraData = false;
                } else if (values[0].charAt(0) == 'b') {
                    mode = SummarizationMode.BLOCKS;
                    needTime = false;
                    needLogType = false;
                    needMaterial = false;
                    needData = false;
                    needPlayer = false;
                    needExtraData = false;
                } else if (values[0].charAt(0) == 'e') {
                    mode = SummarizationMode.ENTITIES;
                    needTime = false;
                    needLogType = false;
                    needMaterial = false;
                    needData = false;
                    needPlayer = false;
                    needExtraData = false;
                } else if (values[0].charAt(0) == 'n') {
                    mode = SummarizationMode.NONE;
                } else {
                    throw new IllegalArgumentException("Wrong summarization mode!");
                }
            } else if (param.equals("created") || param.equals("placed")) {
                logType = LoggingType.blockplace;
            } else if (param.equals("destroyed") || param.equals("breaked")) {
                logType = LoggingType.blockbreak;
            } else if (param.equals("kills")) {
                logType = LoggingType.creaturekill;
                needExtraData = true;
            } else if (param.equals("all")) {
                logType = LoggingType.all;
                needExtraData = true;
            } else if (param.equals("limit")) {
                if (values.length != 1) {
                    throw new IllegalArgumentException("Wrong count of arguments for '" + param + "'");
                }
                if (!isInt(values[0])) {
                    throw new IllegalArgumentException("Not a number: '" + values[0] + "'");
                }
                limit = Integer.parseInt(values[0]);
            } else if (param.equals("worldname")) {
                if (values.length != 1) {
                    throw new IllegalArgumentException("Wrong count of arguments for '" + param + "'");
                }
                final World w = sender.getServer().getWorld(values[0].replace("\"", ""));
                if (w == null) {
                    throw new IllegalArgumentException("There is no world called '" + values[0] + "'");
                }
                world = w;
            } else if (param.equals("asc")) {
                order = Order.ASC;
            } else if (param.equals("desc")) {
                order = Order.DESC;
            } else if (param.equals("coords")) {
                needCoords = true;
            } else if (param.equals("extra")) {
                needExtraData = true;
            } else if (param.equals("loc") || param.equals("location")) {
                final String[] vectors = (values.length == 1) ? values[0].split(":") : values;
                if (vectors.length != 3) {
                    throw new IllegalArgumentException("Wrong count arguments for '" + param + "'");
                }
                for (final String vec : vectors) {
                    if (!isInt(vec)) {
                        throw new IllegalArgumentException("Not a number: '" + vec + "'");
                    }
                }
                location = new Location(null, Integer.valueOf(vectors[0]), Integer.valueOf(vectors[1]), Integer.valueOf(vectors[2]));
                radius = 0;
            } else {
                throw new IllegalArgumentException("Not a valid argument: '" + param + "'");
            }
            i += values.length;
        }

        if (!prepareToolQuery) {
            if (world == null) {
                throw new IllegalArgumentException("No world specified!");
            }
            if (!BlockActivity.isWorldLogged(world.getName())) {
                throw new IllegalArgumentException("This world ('" + world.getName() + "') isn't logged!");
            }
        }
    }

    public static boolean isValidParam(String param) {
        return validParams.contains(param.toLowerCase());
    }

    private String[] getValues(List<String> args, int offset) {
        int i;
        for (i = offset; i < args.size(); i++) {
            if (isValidParam(args.get(i))) {
                break;
            }
        }
        if (i == offset) {
            return new String[0];
        }
        final List<String> values = new ArrayList<String>();
        StringBuilder value = new StringBuilder();
        for (int j = offset; j < i; j++) {
            if (args.get(j).charAt(0) == '\"' || value.length() != 0) {
                if (!args.get(j).endsWith("\"")) {
                    if (args.get(j).charAt(0) == '\"') {
                        value.append(args.get(j).substring(1)).append(' ');
                    } else {
                        value.append(args.get(j)).append(' ');
                    }
                } else {
                    if (args.get(j).charAt(0) == '\"') {
                        value.append(args.get(j).substring(0, args.get(j).length() - 1).substring(1));
                    } else {
                        value.append(args.get(j).substring(0, args.get(j).length() - 1));
                    }
                    values.add(value.toString());
                    value = new StringBuilder();
                }
            } else {
                values.add(args.get(j));
            }
        }
        return values.toArray(new String[values.size()]);
    }
}
