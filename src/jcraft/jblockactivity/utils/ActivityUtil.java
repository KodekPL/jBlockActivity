package jcraft.jblockactivity.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.bukkit.craftbukkit.libs.com.google.gson.Gson;
import org.bukkit.craftbukkit.libs.com.google.gson.GsonBuilder;
import org.bukkit.craftbukkit.libs.com.google.gson.JsonSyntaxException;
import org.bukkit.entity.EntityType;

public class ActivityUtil {

    private final static Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public static String toJson(Object object) {
        return GSON.toJson(object);
    }

    public static <T> T fromJson(String data, Class<T> clazz) {
        try {
            return GSON.fromJson(data, clazz);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String fixUUID(String uuid) {
        if (uuid.length() != 32) {
            return uuid;
        }
        return uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16) + "-" + uuid.substring(16, 20) + "-"
                + uuid.substring(20, 32);
    }

    public static EntityType matchEntity(String name) {
        if (name == null) {
            return null;
        }

        EntityType result = null;

        try {
            result = EntityType.fromId(Integer.parseInt(name));
        } catch (NumberFormatException e) {
        }

        if (result == null) {
            String filtered = name.toUpperCase();

            filtered = filtered.replaceAll("\\s+", "_").replaceAll("\\W", "");
            try {
                result = EntityType.valueOf(filtered);
            } catch (IllegalArgumentException e) {
            }
        }

        return result;
    }

    public static String makeSpaces(int count) {
        final StringBuilder filled = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            filled.append(' ');
        }
        return filled.toString();
    }

    public static int parseTime(String[] sTime) {
        if (sTime == null || sTime.length < 1 || sTime.length > 2) {
            return -1;
        }
        if (sTime.length == 1 && isInt(sTime[0])) {
            return Integer.valueOf(sTime[0]);
        }
        if (!sTime[0].contains(":") && !sTime[0].contains(".")) {
            if (sTime.length == 2) {
                if (!isInt(sTime[0])) {
                    return -1;
                }
                int min = Integer.parseInt(sTime[0]);
                if (sTime[1].charAt(0) == 'h') {
                    min *= 60;
                } else if (sTime[1].charAt(0) == 'd') {
                    min *= 1440;
                }
                return min;
            } else if (sTime.length == 1) {
                int days = 0, hours = 0, minutes = 0;
                int lastIndex = 0, currIndex = 1;
                while (currIndex <= sTime[0].length()) {
                    while (currIndex <= sTime[0].length() && isInt(sTime[0].substring(lastIndex, currIndex))) {
                        currIndex++;
                    }
                    if (currIndex - 1 != lastIndex) {
                        final String param = sTime[0].substring(currIndex - 1, currIndex).toLowerCase();
                        if (param.equals("d")) {
                            days = Integer.parseInt(sTime[0].substring(lastIndex, currIndex - 1));
                        } else if (param.equals("h")) {
                            hours = Integer.parseInt(sTime[0].substring(lastIndex, currIndex - 1));
                        } else if (param.equals("m")) {
                            minutes = Integer.parseInt(sTime[0].substring(lastIndex, currIndex - 1));
                        }
                    }
                    lastIndex = currIndex;
                    currIndex++;
                }
                if (days == 0 && hours == 0 && minutes == 0) {
                    return -1;
                }
                return minutes + hours * 60 + days * 1440;
            } else {
                return -1;
            }
        }
        final String timestamp;
        if (sTime.length == 1) {
            if (sTime[0].contains(":")) {
                timestamp = new SimpleDateFormat("dd.MM.yyyy").format(System.currentTimeMillis()) + " " + sTime[0];
            } else {
                timestamp = sTime[0] + " 00:00:00";
            }
        } else {
            timestamp = sTime[0] + " " + sTime[1];
        }
        try {
            return (int) ((System.currentTimeMillis() - new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").parse(timestamp).getTime()) / 60000);
        } catch (final ParseException ex) {
            return -1;
        }
    }

    private final static SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("MM-dd HH:mm:ss");

    public static String formatTime(long time) {
        return TIME_FORMATTER.format(time);
    }

    public static boolean isInt(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (final NumberFormatException e) {
            return false;
        }
    }

}
