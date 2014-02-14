package jcraft.jblockactivity.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.config.WorldConfig;
import jcraft.jblockactivity.extradata.ExtraLoggingTypes.BlockMetaType;

public class ImportQueryGen {

    public static enum ImportPlugin {
        LOGBLOCK;
    }

    public static boolean createImportFile(ImportPlugin plugin, String oldTable, String newTable) {
        switch (plugin) {
        case LOGBLOCK:
            return createLogBlockImport(oldTable, newTable);
        default:
            return false;
        }
    }

    private static boolean createLogBlockImport(String oldTable, String worldName) {
        final WorldConfig config = BlockActivity.getWorldConfig(worldName);
        if (config == null) {
            return false;
        }
        final List<String> query = new ArrayList<String>();

        /*
         * Query created by Migo2468
         * Compatible with LogBlock 1.80
         */

        query.add("INSERT INTO `ba-players` (`playerid`,`playername`) SELECT `playerid`,`playername` FROM `lb-players`; ");

        if (config.isLoggingExtraBlockMeta(BlockMetaType.signtext)) {
            query.add("INSERT INTO `" + config.tableName
                    + "-extra` (`id`,`data`) SELECT `id`,CONCAT('{\"text\":[\"', REPLACE(signtext, '\\0', '\",\"'), '\"]}') FROM `" + oldTable
                    + "-sign`; ");
        }

        query.add("INSERT INTO `" + config.tableName
                + "` (`id`,`time`,`type`,`playerid`, `old_id`,`old_data`,`new_id`,`new_data`, `x`,`y`,`z`) SELECT `" + oldTable + "`.`id`,`"
                + oldTable + "`.`date`,1,`" + oldTable + "`.`playerid`, `" + oldTable + "`.`replaced`,`" + oldTable + "`.`data`,`" + oldTable
                + "`.`type`,0, `" + oldTable + "`.`x`,`" + oldTable + "`.`y`,`" + oldTable + "`.`z` FROM `" + oldTable + "` WHERE `replaced`=0; ");

        query.add("INSERT INTO `" + config.tableName
                + "` (`id`,`time`,`type`,`playerid`, `old_id`,`old_data`,`new_id`,`new_data`, `x`,`y`,`z`) SELECT `" + oldTable + "`.`id`,`"
                + oldTable + "`.`date`,1,`" + oldTable + "`.`playerid`, `" + oldTable + "`.`replaced`,`" + oldTable + "`.`data`,`" + oldTable
                + "`.`type`,0, `" + oldTable + "`.`x`,`" + oldTable + "`.`y`,`" + oldTable + "`.`z` FROM `" + oldTable + "` WHERE `type`=0; ");

        query.add("INSERT INTO `" + config.tableName
                + "` (`id`,`time`,`type`,`playerid`, `old_id`,`old_data`,`new_id`,`new_data`, `x`,`y`,`z`) SELECT `" + oldTable + "`.`id`,`"
                + oldTable + "`.`date`,1,`" + oldTable + "`.`playerid`, `" + oldTable + "`.`replaced`,`" + oldTable + "`.`data`,`" + oldTable
                + "`.`type`,0, `" + oldTable + "`.`x`,`" + oldTable + "`.`y`,`" + oldTable + "`.`z` FROM `" + oldTable + "` WHERE `replaced`=`type`;");

        saveFile(query);
        return true;
    }

    private static void saveFile(List<String> list) {
        final FileWriter writer;
        final File file = new File(BlockActivity.dataFolder, "LogBlockImport.sql");
        try {
            writer = new FileWriter(file);
            for (String str : list) {
                writer.write(str);
            }
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
