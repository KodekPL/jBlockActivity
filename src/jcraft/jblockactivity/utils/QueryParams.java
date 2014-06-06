package jcraft.jblockactivity.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.LoggingType;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;

public class QueryParams {

    private static final Set<String> VALID_PARAMS = new HashSet<String>(Arrays.asList("player", "block", "type", "entity", "area", "time", "since",
            "before", "sum", "created", "placed", "destroyed", "breaked", "kills", "all", "limit", "worldname", "asc", "desc", "coords", "extra",
            "location", "loc"));

    public static enum ParamType {
        PREPARE_QUERY_TOOL,
        WORLD,
        PLAYERS,
        EXCLUDED_PLAYERS,
        BLOCK_TYPES,
        ENTITY_TYPES,
        AREA_RADIUS,
        LOCATION,
        SINCE,
        BEFORE,
        SUM_MODE,
        NEED_TIME,
        NEED_LOG_TYPE,
        NEED_MATERIAL,
        NEED_DATA,
        NEED_PLAYER,
        NEED_COORDS,
        NEED_EXTRA_DATA,
        LOG_TYPE,
        LIMIT,
        ORDER;
    }

    public static enum SummarizationMode {
        NONE, PLAYERS, BLOCKS, ENTITIES;
    }

    public static enum Order {
        ASC, DESC;
    }

    private final Map<ParamType, Object> PARAMS = new HashMap<ParamType, Object>();

    public boolean prepareToolQuery = false;

    public QueryParams(CommandSender sender, String[] args, boolean prepareToolQuery) throws IllegalArgumentException {
        setParam(ParamType.NEED_TIME, true);
        setParam(ParamType.NEED_LOG_TYPE, true);
        setParam(ParamType.NEED_MATERIAL, true);
        setParam(ParamType.NEED_DATA, true);
        setParam(ParamType.NEED_PLAYER, true);

        final List<String> argsList = new ArrayList<String>(Arrays.asList(args));
        if (!isValidParam(argsList.get(0))) {
            argsList.remove(0);
        }
        this.prepareToolQuery = prepareToolQuery;
        parseArgs(sender, argsList.toArray(new String[argsList.size()]));
    }

    public QueryParams(String args) throws IllegalArgumentException {
        this(null, args.trim().split(" "), false);
    }

    public boolean isValidParam(String param) {
        return VALID_PARAMS.contains(param.toLowerCase());
    }

    public void setParam(ParamType type, Object value) {
        PARAMS.put(type, value);
    }

    public Object getParam(ParamType type) {
        return PARAMS.get(type);
    }

    public boolean getBoolean(ParamType type) {
        final Object object = getParam(type);
        if (object != null && object instanceof Boolean) {
            return (boolean) object;
        }
        return false;
    }

    public boolean containsParam(ParamType type) {
        return PARAMS.containsKey(type);
    }

    public World getWorld() {
        return (World) PARAMS.get(ParamType.WORLD);
    }

    public void setLocation(Location location) {
        setParam(ParamType.LOCATION, location);
        setParam(ParamType.WORLD, location.getWorld());
    }

    public String getTable() {
        return "`" + BlockActivity.getWorldTableName(getWorld().getName()) + "`";
    }

    public String getTable(String suffix) {
        return "`" + BlockActivity.getWorldTableName(getWorld().getName()) + suffix + "`";
    }

    public String getLimit() {
        final Object limitObject = getParam(ParamType.LIMIT);
        final int limit = (limitObject == null) ? 0 : (int) limitObject;
        return (limit > 0) ? "LIMIT " + limit : "";
    }

    public Order getOrder() {
        if (containsParam(ParamType.ORDER)) {
            return (Order) getParam(ParamType.ORDER);
        } else {
            return Order.DESC;
        }
    }

    public SummarizationMode getSumMode() {
        final Object object = getParam(ParamType.SUM_MODE);
        if (object == null) {
            return SummarizationMode.NONE;
        }
        return (SummarizationMode) object;
    }

    public String getQuery() {
        final StringBuilder builder = new StringBuilder();
        if (getSumMode() == SummarizationMode.NONE) {
            builder.append("SELECT ");
            if (getBoolean(ParamType.NEED_TIME)) builder.append("time, ");
            if (getBoolean(ParamType.NEED_LOG_TYPE)) builder.append("type, ");
            if (getBoolean(ParamType.NEED_MATERIAL)) builder.append("old_id, new_id, ");
            if (getBoolean(ParamType.NEED_DATA)) builder.append("old_data, new_data, ");
            if (getBoolean(ParamType.NEED_PLAYER)) builder.append("playername, uuid, ");
            if (getBoolean(ParamType.NEED_COORDS)) builder.append("x, y, z, ");
            if (getBoolean(ParamType.NEED_EXTRA_DATA)) builder.append("data, ");
            builder.delete(builder.length() - 2, builder.length());
            builder.append(" FROM ").append(getTable()).append(' ');
            if (getBoolean(ParamType.NEED_PLAYER) || containsParam(ParamType.PLAYERS) || containsParam(ParamType.EXCLUDED_PLAYERS)) {
                builder.append("INNER JOIN `ba-players` USING (playerid) ");
            }
            if (getBoolean(ParamType.NEED_EXTRA_DATA)) {
                builder.append("LEFT JOIN " + getTable("-extra") + " USING (id) ");
            }
            builder.append(getWhere(containsParam(ParamType.LOG_TYPE) ? (LoggingType) getParam(ParamType.LOG_TYPE) : LoggingType.bothblocks));
            builder.append("ORDER BY time ").append(getOrder()).append(", id ").append(getOrder()).append(' ').append(getLimit());
        } else if (getSumMode() == SummarizationMode.BLOCKS) {
            return "SELECT new_id, SUM(created) AS created, SUM(destroyed) AS destroyed FROM ((SELECT new_id, count(*) AS created, 0 AS destroyed FROM "
                    + getTable()
                    + " INNER JOIN `ba-players` USING (playerid) "
                    + getWhere(LoggingType.blockplace)
                    + "GROUP BY new_id) UNION (SELECT old_id AS new_id, 0 AS created, count(*) AS destroyed FROM "
                    + getTable()
                    + " INNER JOIN `ba-players` USING (playerid) "
                    + getWhere(LoggingType.blockbreak)
                    + "GROUP BY old_id)) AS t GROUP BY new_id ORDER BY SUM(created) + SUM(destroyed) " + getOrder() + " " + getLimit();
        } else if (getSumMode() == SummarizationMode.ENTITIES) {
            return "SELECT old_id, SUM(killed) AS killed FROM (SELECT old_id, count(*) AS killed FROM " + getTable()
                    + " INNER JOIN `ba-players` USING (playerid) " + getWhere(LoggingType.creaturekill)
                    + "GROUP BY old_id) AS t GROUP BY old_id ORDER BY SUM(killed) " + getOrder() + " " + getLimit();
        } else if (getSumMode() == SummarizationMode.PLAYERS) {
            return "SELECT playername, SUM(created) AS created, SUM(destroyed) AS destroyed FROM ((SELECT playerid, count(*) AS created, 0 AS destroyed FROM "
                    + getTable()
                    + " "
                    + getWhere(LoggingType.blockplace)
                    + "GROUP BY playerid) UNION (SELECT playerid, 0 AS created, count(*) AS destroyed FROM "
                    + getTable()
                    + " "
                    + getWhere(LoggingType.blockbreak)
                    + "GROUP BY playerid)) AS t INNER JOIN `ba-players` USING (playerid) GROUP BY playerid ORDER BY SUM(created) + SUM(destroyed) "
                    + getOrder() + " " + getLimit();
        }

        return builder.toString();
    }

    public String getWhere(LoggingType logType) {
        final StringBuilder builder = new StringBuilder("WHERE ");
        if (logType == LoggingType.all) {
            final Object object = getParam(ParamType.BLOCK_TYPES);
            if (object != null) {
                final Set<MaterialData> blockTypes = (HashSet<MaterialData>) object;
                builder.append('(');
                for (MaterialData block : blockTypes) {
                    builder.append("((new_id = ").append(block.getItemTypeId());
                    if (block.getData() != -1) {
                        builder.append(" AND new_data = ").append(block.getData()).append(')');
                    } else {
                        builder.append(')');
                    }
                    builder.append(" OR (old_id = ").append(block.getItemTypeId());
                    if (block.getData() != -1) {
                        builder.append(" AND old_data = ").append(block.getData()).append(')');
                    } else {
                        builder.append(')');
                    }
                    builder.append(") OR ");
                }
                builder.delete(builder.length() - 4, builder.length());
                builder.append(") AND ");
            }
        } else if (logType == LoggingType.bothblocks) {
            final Object object = getParam(ParamType.BLOCK_TYPES);
            if (object != null) {
                final Set<MaterialData> blockTypes = (HashSet<MaterialData>) object;
                builder.append('(');
                for (final MaterialData block : blockTypes) {
                    builder.append("((new_id = ").append(block.getItemTypeId());
                    if (block.getData() != -1) {
                        builder.append(" AND new_data = ").append(block.getData()).append(')');
                    } else {
                        builder.append(')');
                    }
                    builder.append(" OR (old_id = ").append(block.getItemTypeId());
                    if (block.getData() != -1) {
                        builder.append(" AND old_data = ").append(block.getData()).append(')');
                    } else {
                        builder.append(')');
                    }
                    builder.append(") OR ");
                }
                builder.delete(builder.length() - 4, builder.length());
                builder.append(" AND ").append("(type = ").append(LoggingType.blockplace.getId()).append(" OR type = ")
                        .append(LoggingType.blockbreak.getId()).append(")) AND ");
            } else {
                builder.append("(type = ").append(LoggingType.blockplace.getId()).append(" OR type = ").append(LoggingType.blockbreak.getId())
                        .append(") AND ");
            }
        } else if (logType == LoggingType.blockplace) {
            final Object object = getParam(ParamType.BLOCK_TYPES);
            if (object != null) {
                final Set<MaterialData> blockTypes = (HashSet<MaterialData>) object;
                builder.append('(');
                for (final MaterialData block : blockTypes) {
                    builder.append("((new_id = ").append(block.getItemTypeId());
                    if (block.getData() != -1) {
                        builder.append(") AND new_data = ").append(block.getData());
                    } else {
                        builder.append(')');
                    }
                    builder.append(") OR ");
                }
                builder.delete(builder.length() - 4, builder.length());
                builder.append(" AND ").append("type = ").append(LoggingType.blockplace.getId()).append(") AND ");
            } else {
                builder.append("type = ").append(LoggingType.blockplace.getId()).append(" AND ");
            }
        } else if (logType == LoggingType.blockbreak) {
            final Object object = getParam(ParamType.BLOCK_TYPES);
            if (object != null) {
                final Set<MaterialData> blockTypes = (HashSet<MaterialData>) object;
                builder.append('(');
                for (final MaterialData block : blockTypes) {
                    builder.append("((old_id = ").append(block.getItemTypeId());
                    if (block.getData() != -1) {
                        builder.append(") AND old_data = ").append(block.getData());
                    } else {
                        builder.append(')');
                    }
                    builder.append(") OR ");
                }
                builder.delete(builder.length() - 4, builder.length());
                builder.append(" AND ").append("type = ").append(LoggingType.blockbreak.getId()).append(") AND ");
            } else {
                builder.append("type = ").append(LoggingType.blockbreak.getId()).append(" AND ");
            }
        } else if (logType == LoggingType.creaturekill) {
            final Object object = getParam(ParamType.ENTITY_TYPES);
            if (object != null) {
                final Set<EntityType> entityTypes = (HashSet<EntityType>) object;
                builder.append('(');
                for (final EntityType entity : entityTypes) {
                    builder.append("((old_id = ").append(entity.getTypeId()).append(')').append(") OR ");
                }
                builder.delete(builder.length() - 4, builder.length());
                builder.append(" AND ").append("type = ").append(LoggingType.creaturekill.getId()).append(") AND ");
            } else {
                builder.append("type = ").append(LoggingType.creaturekill.getId()).append(" AND ");
            }
        }

        final Object object = getParam(ParamType.LOCATION);
        if (object != null) {
            final Location location = (Location) object;
            final int radius = (getParam(ParamType.AREA_RADIUS) == null) ? 0 : (int) getParam(ParamType.AREA_RADIUS);
            if (radius == 0) {
                compileLocationQuery(builder, location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getBlockX(),
                        location.getBlockY(), location.getBlockZ());

            } else if (radius > 0) {
                compileLocationQuery(builder, location.getBlockX() - radius + 1, location.getBlockY() - radius + 1,
                        location.getBlockZ() - radius + 1, location.getBlockX() + radius - 1, location.getBlockY() + radius - 1, location.getBlockZ()
                                + radius - 1);
            }
        }

        if (getSumMode() != SummarizationMode.PLAYERS) {
            final Object playersObject = getParam(ParamType.PLAYERS);
            final Object exPlayersObject = getParam(ParamType.EXCLUDED_PLAYERS);
            if (playersObject != null) {
                final Set<String> players = (HashSet<String>) playersObject;
                builder.append('(');
                for (final String playerName : players) {
                    builder.append("playername = '").append(playerName).append("' OR ");
                }
                builder.delete(builder.length() - 4, builder.length());
                builder.append(") AND ");
            }
            if (exPlayersObject != null) {
                final Set<String> exPlayers = (HashSet<String>) exPlayersObject;
                builder.append('(');
                for (final String playerName : exPlayers) {
                    builder.append("playername != '").append(playerName).append("' OR ");
                }
                builder.delete(builder.length() - 4, builder.length());
                builder.append(") AND ");
            }
        }

        final Object sinceObject = getParam(ParamType.SINCE);
        if (sinceObject != null && ((int) sinceObject) > 0) {
            builder.append("time > date_sub(now(), INTERVAL ").append(((int) sinceObject)).append(" MINUTE) AND ");
        }
        final Object beforeObject = getParam(ParamType.BEFORE);
        if (beforeObject != null && ((int) beforeObject) > 0) {
            builder.append("time < date_sub(now(), INTERVAL ").append(((int) beforeObject)).append(" MINUTE) AND ");
        }
        if (builder.length() > 6) {
            builder.delete(builder.length() - 4, builder.length());
        } else {
            builder.delete(0, builder.length());
        }

        return builder.toString();
    }

    private void compileLocationQuery(StringBuilder builder, int blockX, int blockY, int blockZ, int blockX2, int blockY2, int blockZ2) {
        compileLocationQueryPart(builder, "x", blockX, blockX2);
        builder.append(" AND ");
        compileLocationQueryPart(builder, "y", blockY, blockY2);
        builder.append(" AND ");
        compileLocationQueryPart(builder, "z", blockZ, blockZ2);
        builder.append(" AND ");
    }

    private void compileLocationQueryPart(StringBuilder builder, String locValue, int loc, int loc2) {
        final int min = Math.min(loc, loc2);
        final int max = Math.max(loc2, loc);

        if (min == max) {
            builder.append(locValue).append(" = ").append(min);
        } else if (max - min > 50) {
            builder.append(locValue).append(" >= ").append(min).append(" AND ").append(locValue).append(" <= ").append(max);
        } else {
            builder.append(locValue).append(" in (");
            for (int c = min; c < max; c++) {
                builder.append(c).append(',');
            }
            builder.append(max);
            builder.append(')');
        }
    }

    public void parseArgs(CommandSender sender, String[] args) throws IllegalArgumentException {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("No parameters specified!");
        }

        final Player player = (sender instanceof Player) ? (Player) sender : null;
        if (player != null && getWorld() == null) {
            setParam(ParamType.WORLD, player.getWorld());
        }

        for (int i = 0; i < args.length; i++) {
            final String param = args[i].toLowerCase();
            final String[] values = getValues(args, i + 1);

            //
            if (param.equals("player")) {
                playerParam(param, values);
            } else if (param.equals("block") || param.equals("type")) {
                blockTypeParam(param, values);
            } else if (param.equals("entity")) {
                entityParam(param, values);
            } else if (param.equals("area")) {
                areaParam(param, values, player);
            } else if (param.equals("time") || param.equals("since")) {
                sinceParam(param, values);
            } else if (param.equals("before")) {
                beforeParam(param, values);
            } else if (param.equals("sum")) {
                sumParam(param, values);
            } else if (param.equals("created") || param.equals("placed")) {
                setParam(ParamType.LOG_TYPE, LoggingType.blockplace);
            } else if (param.equals("destroyed") || param.equals("breaked")) {
                setParam(ParamType.LOG_TYPE, LoggingType.blockbreak);
            } else if (param.equals("kills")) {
                setParam(ParamType.LOG_TYPE, LoggingType.creaturekill);
                setParam(ParamType.NEED_EXTRA_DATA, true);
            } else if (param.equals("all")) {
                setParam(ParamType.LOG_TYPE, LoggingType.all);
            } else if (param.equals("limit")) {
                limitParam(param, values);
            } else if (param.equals("worldname")) {
                worldNameParam(param, values);
            } else if (param.equals("asc")) {
                setParam(ParamType.ORDER, Order.ASC);
            } else if (param.equals("desc")) {
                setParam(ParamType.ORDER, Order.DESC);
            } else if (param.equals("coords")) {
                setParam(ParamType.NEED_COORDS, true);
            } else if (param.equals("extra")) {
                setParam(ParamType.NEED_EXTRA_DATA, true);
            } else if (param.equals("loc") || param.equals("location")) {
                locationParam(param, values);
            } else {
                throw new IllegalArgumentException("Not a valid argument: '" + param + "'");
            }
            //

            i += values.length;
        }

        if (!prepareToolQuery) {
            if (getWorld() == null) {
                throw new IllegalArgumentException("No world specified!");
            }
            if (!BlockActivity.isWorldLogged(getWorld().getName())) {
                throw new IllegalArgumentException("This world ('" + getWorld().getName() + "') isn't logged!");
            }
        }
    }

    private void locationParam(String param, String[] values) throws IllegalArgumentException {
        final String[] vectors = (values.length == 1) ? values[0].split(",") : values;
        if (vectors.length != 3) {
            throw new IllegalArgumentException("Wrong count arguments for '" + param + "'");
        }
        for (final String vec : vectors) {
            if (!ActivityUtil.isInt(vec)) {
                throw new IllegalArgumentException("Not a number: '" + vec + "'");
            }
        }
        final Location location = new Location(null, Integer.valueOf(vectors[0]), Integer.valueOf(vectors[1]), Integer.valueOf(vectors[2]));
        setParam(ParamType.LOCATION, location);
        setParam(ParamType.AREA_RADIUS, 0);
    }

    private void worldNameParam(String param, String[] values) throws IllegalArgumentException {
        if (values.length != 1) {
            throw new IllegalArgumentException("Wrong count of arguments for '" + param + "'");
        }
        final World world = Bukkit.getWorld(values[0].replace("\"", ""));
        if (world == null) {
            throw new IllegalArgumentException("There is no world called '" + values[0] + "'");
        }
        setParam(ParamType.WORLD, world);
    }

    private void limitParam(String param, String[] values) throws IllegalArgumentException {
        if (values.length != 1) {
            throw new IllegalArgumentException("Wrong count of arguments for '" + param + "'");
        }
        if (!ActivityUtil.isInt(values[0])) {
            throw new IllegalArgumentException("Not a number: '" + values[0] + "'");
        }
        setParam(ParamType.LIMIT, Integer.parseInt(values[0]));
    }

    private void sumParam(String param, String[] values) throws IllegalArgumentException {
        if (values.length != 1) {
            throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
        }
        if (values[0].charAt(0) == 'p') {
            setParam(ParamType.SUM_MODE, SummarizationMode.PLAYERS);
            setParam(ParamType.NEED_TIME, false);
            setParam(ParamType.NEED_LOG_TYPE, false);
            setParam(ParamType.NEED_MATERIAL, false);
            setParam(ParamType.NEED_DATA, false);
            setParam(ParamType.NEED_PLAYER, false);
            setParam(ParamType.NEED_EXTRA_DATA, false);
        } else if (values[0].charAt(0) == 'b') {
            setParam(ParamType.SUM_MODE, SummarizationMode.BLOCKS);
            setParam(ParamType.NEED_TIME, false);
            setParam(ParamType.NEED_LOG_TYPE, false);
            setParam(ParamType.NEED_MATERIAL, false);
            setParam(ParamType.NEED_DATA, false);
            setParam(ParamType.NEED_PLAYER, false);
            setParam(ParamType.NEED_EXTRA_DATA, false);
        } else if (values[0].charAt(0) == 'e') {
            setParam(ParamType.SUM_MODE, SummarizationMode.ENTITIES);
            setParam(ParamType.NEED_TIME, false);
            setParam(ParamType.NEED_LOG_TYPE, false);
            setParam(ParamType.NEED_MATERIAL, false);
            setParam(ParamType.NEED_DATA, false);
            setParam(ParamType.NEED_PLAYER, false);
            setParam(ParamType.NEED_EXTRA_DATA, false);
        } else if (values[0].charAt(0) == 'n') {
            setParam(ParamType.SUM_MODE, SummarizationMode.NONE);
        } else {
            throw new IllegalArgumentException("Wrong summarization mode!");
        }
    }

    private void beforeParam(String param, String[] values) throws IllegalArgumentException {
        final int before = (values.length > 0) ? ActivityUtil.parseTime(values) : BlockActivity.config.defaultTime;
        if (before == -1) {
            throw new IllegalArgumentException("Failed to parse time spec for '" + param + "'");
        }
        setParam(ParamType.BEFORE, before);
    }

    private void sinceParam(String param, String[] values) throws IllegalArgumentException {
        final int since = (values.length > 0) ? ActivityUtil.parseTime(values) : BlockActivity.config.defaultTime;
        if (since == -1) {
            throw new IllegalArgumentException("Failed to parse time spec for '" + param + "'");
        }
        setParam(ParamType.SINCE, since);
    }

    private void areaParam(String param, String[] values, Player player) throws IllegalArgumentException {
        if (player == null && !prepareToolQuery) {
            throw new IllegalArgumentException("You have to ba a player to use area!");
        }
        if (values.length == 0) {
            setParam(ParamType.AREA_RADIUS, BlockActivity.config.defaultDistance);
            if (!prepareToolQuery) {
                setParam(ParamType.LOCATION, player.getLocation());
            }
        } else {
            if (!ActivityUtil.isInt(values[0])) {
                throw new IllegalArgumentException("Not a number: '" + values[0] + "'");
            }
            setParam(ParamType.AREA_RADIUS, Integer.parseInt(values[0]));
            if (!prepareToolQuery) {
                setParam(ParamType.LOCATION, player.getLocation());
            }
        }
    }

    private void entityParam(String param, String[] values) throws IllegalArgumentException {
        if (values.length < 1) {
            throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
        }
        final Set<EntityType> entityTypes = new HashSet<EntityType>();
        for (final String entityName : values) {
            final EntityType ent = ActivityUtil.matchEntity(entityName);
            if (ent == null) {
                throw new IllegalArgumentException("No entity matching: '" + entityName + "'");
            }
            entityTypes.add(ent);
        }
        if (!entityTypes.isEmpty()) {
            setParam(ParamType.NEED_EXTRA_DATA, true);
            setParam(ParamType.LOG_TYPE, LoggingType.creaturekill);
            setParam(ParamType.ENTITY_TYPES, entityTypes);
        }
    }

    private void blockTypeParam(String param, String[] values) throws IllegalArgumentException {
        if (values.length < 1) {
            throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
        }
        final Set<MaterialData> blockTypes = new HashSet<MaterialData>();
        for (final String blockName : values) {
            if (blockName.contains(":")) {
                String[] splitter = blockName.split(":");
                if (splitter.length > 2) {
                    throw new IllegalArgumentException("No material matching: '" + blockName + "'");
                }

                final Material mat = Material.matchMaterial(splitter[0]);
                if (mat == null) {
                    throw new IllegalArgumentException("No material matching: '" + blockName + "'");
                }

                final int data;
                try {
                    data = Integer.parseInt(splitter[1]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Data type not a valid number: '" + splitter[1] + "'");
                }
                if (data > 255 || data < 0) {
                    throw new IllegalArgumentException("Data type out of range (0-255): '" + data + "'");
                }

                if (BlocksUtil.hasExtraData(mat)) {
                    setParam(ParamType.NEED_EXTRA_DATA, true);
                }
                blockTypes.add(new MaterialData(mat, (byte) data));
            } else {
                final Material mat = Material.matchMaterial(blockName);
                if (mat == null) {
                    throw new IllegalArgumentException("No material matching: '" + blockName + "'");
                }
                if (BlocksUtil.hasExtraData(mat)) {
                    setParam(ParamType.NEED_EXTRA_DATA, true);
                }
                blockTypes.add(new MaterialData(mat, (byte) -1));
            }
        }
        if (!blockTypes.isEmpty()) {
            setParam(ParamType.BLOCK_TYPES, blockTypes);
        }
    }

    private void playerParam(String param, String[] values) throws IllegalArgumentException {
        if (values.length < 1) {
            throw new IllegalArgumentException("No or wrong count of arguments for '" + param + "'");
        }
        final Set<String> players = new HashSet<String>();
        final Set<String> excludedPlayers = new HashSet<String>();
        for (String playerName : values) {
            if (playerName.length() > 0) {
                String name;
                if (playerName.charAt(0) == '"' && playerName.charAt(playerName.length() - 1) == '"') {
                    name = playerName.replaceAll("[^a-zA-Z0-9_]", "");
                } else {
                    final List<Player> matches = Bukkit.matchPlayer(playerName);
                    if (matches.size() > 1) {
                        throw new IllegalArgumentException("Inaccurate playername '" + playerName + "'");
                    }
                    name = (matches.size() == 1) ? matches.get(0).getName() : playerName.replaceAll("[^a-zA-Z0-9_]", "");
                }

                if (playerName.charAt(0) == '!') {
                    excludedPlayers.add(name);
                } else {
                    players.add(name);
                }
            }
        }
        if (!players.isEmpty()) {
            setParam(ParamType.PLAYERS, players);
            setParam(ParamType.NEED_PLAYER, true);
        }
        if (!excludedPlayers.isEmpty()) {
            setParam(ParamType.EXCLUDED_PLAYERS, excludedPlayers);
            setParam(ParamType.NEED_PLAYER, true);
        }
    }

    private String[] getValues(String[] args, int offset) {
        int i;
        for (i = offset; i < args.length; i++) {
            if (isValidParam(args[i])) {
                break;
            }
        }
        if (i == offset) {
            return new String[0];
        }
        final List<String> values = new ArrayList<String>();
        StringBuilder value = new StringBuilder();
        for (int j = offset; j < i; j++) {
            if (args[j].charAt(0) == '\"' || value.length() != 0) {
                if (!args[j].endsWith("\"")) {
                    if (args[j].charAt(0) == '\"') {
                        value.append(args[j].substring(1)).append(' ');
                    } else {
                        value.append(args[j]).append(' ');
                    }
                } else {
                    if (args[j].charAt(0) == '\"') {
                        value.append(args[j].substring(0, args[j].length() - 1).substring(1));
                    } else {
                        value.append(args[j].substring(0, args[j].length() - 1));
                    }
                    values.add(value.toString());
                    value = new StringBuilder();
                }
            } else {
                values.add(args[j]);
            }
        }
        return values.toArray(new String[values.size()]);
    }

}
