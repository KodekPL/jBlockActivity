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

        final List<String> query1 = new ArrayList<String>();
        final List<String> query2 = new ArrayList<String>();

        /*
         * Query created by Migo2468
         * Compatible with LogBlock 1.80
         */

        query1.add("INSERT INTO `ba-players` (`playerid`,`playername`) SELECT `playerid`,`playername` FROM `lb-players`; ");

        if (config.isLoggingExtraBlockMeta(BlockMetaType.signtext)) {
            query2.add("INSERT INTO `" + config.tableName
                    + "-extra` (`id`,`data`) SELECT `id`,CONCAT('{\"text\":[\"', REPLACE(signtext, '\\0', '\",\"'), '\"]}') FROM `" + oldTable
                    + "-sign`; ");
        }

        query2.add("INSERT INTO `" + config.tableName
                + "` (`id`,`time`,`type`,`playerid`, `old_id`,`old_data`,`new_id`,`new_data`, `x`,`y`,`z`) SELECT `" + oldTable + "`.`id`,`"
                + oldTable + "`.`date`,1,`" + oldTable + "`.`playerid`, `" + oldTable + "`.`replaced`,`" + oldTable + "`.`data`,`" + oldTable
                + "`.`type`,0, `" + oldTable + "`.`x`,`" + oldTable + "`.`y`,`" + oldTable + "`.`z` FROM `" + oldTable + "` WHERE `" + oldTable
                + "`.`type`=0 AND `" + oldTable + "`.`replaced`!=0; ");

        query2.add("INSERT INTO `" + config.tableName
                + "` (`id`,`time`,`type`,`playerid`, `old_id`,`old_data`,`new_id`,`new_data`, `x`,`y`,`z`) SELECT `" + oldTable + "`.`id`,`"
                + oldTable + "`.`date`,2,`" + oldTable + "`.`playerid`, `" + oldTable + "`.`replaced`,`" + oldTable + "`.`data`,`" + oldTable
                + "`.`type`,0, `" + oldTable + "`.`x`,`" + oldTable + "`.`y`,`" + oldTable + "`.`z` FROM `" + oldTable + "` WHERE `" + oldTable
                + "`.`replaced` IN (0, 8, 9, 10, 11, 78) AND `" + oldTable + "`.`type`!=0; ");

        query2.add("INSERT INTO `" + config.tableName
                + "` (`id`,`time`,`type`,`playerid`, `old_id`,`old_data`,`new_id`,`new_data`, `x`,`y`,`z`) SELECT `" + oldTable + "`.`id`,`"
                + oldTable + "`.`date`,4,`" + oldTable + "`.`playerid`, `" + oldTable + "`.`replaced`,`" + oldTable + "`.`data`,`" + oldTable
                + "`.`type`,0, `" + oldTable + "`.`x`,`" + oldTable + "`.`y`,`" + oldTable + "`.`z` FROM `" + oldTable + "` WHERE `" + oldTable
                + "`.`replaced`!=0 AND `" + oldTable + "`.`type`!=0 AND `" + oldTable + "`.`replaced`=`" + oldTable + "`.`type`; ");

        query2.add("OPTIMIZE TABLE `" + config.tableName + "`;");

        saveFile(query1, "LogBlockImport_lb-players");
        saveFile(query2, "LogBlockImport_" + oldTable);
        return true;
    }

    private static void saveFile(List<String> list, String fileName) {
        final FileWriter writer;
        final File file = new File(BlockActivity.DATA_FOLDER, fileName + ".sql");

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
