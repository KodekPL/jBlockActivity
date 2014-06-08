package jcraft.jblockactivity.utils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.LoggingType;
import jcraft.jblockactivity.config.ActivityConfig;
import jcraft.jblockactivity.config.WorldConfig;
import jcraft.jblockactivity.extradata.InventoryExtraData;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import com.mojang.api.profiles.HttpProfileRepository;
import com.mojang.api.profiles.Profile;
import com.mojang.api.profiles.ProfileRepository;

public class SQLUpdater implements Runnable {

    private final int LOADED_VERSION;

    public SQLUpdater(int version) {
        LOADED_VERSION = version;
    }

    @Override
    public void run() {
        // SQL UUID Update
        if (LOADED_VERSION < 2) {
            Bukkit.getLogger().log(Level.INFO, "[jBlockActivity] SQLUpdater started updating to config version 2, that can take a while...");
            final Connection connection = BlockActivity.getBlockActivity().getConnection();
            try {
                connection.setAutoCommit(false);
                final Statement state1 = connection.createStatement();
                Bukkit.getLogger().log(Level.INFO, "[jBlockActivity] Updating ba-players table to save players UUID...");
                state1.execute("ALTER TABLE `ba-players` ADD uuid VARCHAR(32) UNIQUE;");
                connection.commit();
                state1.close();

                final Statement state2 = connection.createStatement();
                Bukkit.getLogger().log(Level.INFO, "[jBlockActivity] Fetching all player names from database...");
                final ResultSet result = state2.executeQuery("SELECT * FROM `ba-players` WHERE uuid IS NULL;");
                connection.commit();
                final Map<String, Integer> playerIds = new HashMap<String, Integer>();
                if (result.next()) {
                    result.beforeFirst();
                    while (result.next()) {
                        playerIds.put(result.getString("playername"), result.getInt("playerid"));
                    }
                }
                state2.close();
                Bukkit.getLogger().log(Level.INFO, "[jBlockActivity] Fetched " + playerIds.size() + " names from database.");

                final ProfileRepository repo = new HttpProfileRepository("minecraft");
                Bukkit.getLogger().log(Level.INFO, "[jBlockActivity] Fetching players UUID from Mojang database...");
                final Profile[] profiles = repo.findProfilesByNames(playerIds.keySet().toArray(new String[playerIds.size()]));

                final Statement state3 = connection.createStatement();
                for (Profile profile : profiles) {
                    if (profile == null || !playerIds.containsKey(profile.getName())) {
                        continue;
                    }
                    int playerId = playerIds.get(profile.getName());
                    state3.executeUpdate("UPDATE `ba-players` SET `uuid` = '" + profile.getId() + "' WHERE `playerid` = " + playerId);
                    Bukkit.getLogger()
                            .log(Level.INFO,
                                    "[jBlockActivity] Player " + profile.getName() + "(" + playerId + ") has been updated to UUID ("
                                            + profile.getId() + ").");
                    playerIds.remove(profile.getName());
                }

                for (Entry<String, Integer> entry : playerIds.entrySet()) {
                    state3.executeUpdate("UPDATE `ba-players` SET `uuid` = '" + entry.getKey() + "' WHERE `playerid` = " + entry.getValue());
                    Bukkit.getLogger().log(Level.WARNING, "[jBlockActivity] Could not find UUID for player " + entry.getKey() + ".");
                }

                connection.commit();
                state3.close();
                connection.close();

                Bukkit.getLogger().log(Level.INFO, "[jBlockActivity] Converting players to UUID system has been completed.");
            } catch (final SQLException e) {
                Bukkit.getLogger().log(Level.SEVERE, "[SQLUpdater] Error: ", e);
                return;
            }
        }

        // New inventory items format coverter
        if (LOADED_VERSION < 3) {
            Bukkit.getLogger().log(Level.INFO, "[jBlockActivity] SQLUpdater started updating to config version 3, that can take a while...");
            final Connection connection = BlockActivity.getBlockActivity().getConnection();
            for (WorldConfig config : BlockActivity.worldConfigs.values()) {
                try {
                    connection.setAutoCommit(false);
                    Bukkit.getLogger().log(Level.INFO, "[jBlockActivity] Downloading " + config.tableName + " table to convert inventories...");
                    final Statement state1 = connection.createStatement();
                    final ResultSet result = state1.executeQuery("SELECT id, data FROM `" + config.tableName + "` INNER JOIN `" + config.tableName
                            + "-extra` USING (id) WHERE type = " + LoggingType.inventoryaccess.getId());
                    connection.commit();

                    if (result.next()) {
                        int count = 0;
                        Bukkit.getLogger().log(Level.INFO, "[jBlockActivity] Converting inventory extra data in " + config.tableName + " table.");

                        final Statement state2 = connection.createStatement();
                        result.beforeFirst();
                        while (result.next()) {
                            OldInventoryExtraData oldInvExtraData = ActivityUtil.fromJson(result.getString(2), OldInventoryExtraData.class);
                            if (oldInvExtraData.isEmpty()) {
                                continue;
                            }
                            InventoryExtraData newInvExtraData = new InventoryExtraData(oldInvExtraData.getContent(), false, config);
                            state2.executeUpdate("UPDATE `" + config.tableName + "-extra` SET `data`='" + newInvExtraData.getData()
                                    + "' WHERE `id` = " + result.getInt(1));
                            count++;
                        }

                        Bukkit.getLogger().log(Level.INFO,
                                "[jBlockActivity] Updating " + count + " inventory extra data in " + config.tableName + " table.");

                        connection.commit();
                        state2.close();
                    } else {
                        Bukkit.getLogger().log(Level.INFO, "[jBlockActivity] No inventory extra data in  " + config.tableName + " table, skipped.");
                    }

                    Bukkit.getLogger()
                            .log(Level.INFO, "[jBlockActivity] Finished converting inventory extra data in " + config.tableName + " table.");
                    state1.close();
                    result.close();

                } catch (final SQLException e) {
                    Bukkit.getLogger().log(Level.SEVERE, "[SQLUpdater] Error: ", e);
                    return;
                }
            }
        }

        /**
         * @note Do future config/table updates here
         */

        updateVersion();
    }

    private void updateVersion() {
        if (BlockActivity.LATEST_VERSION > LOADED_VERSION) {
            final YamlConfiguration config = YamlConfiguration.loadConfiguration(ActivityConfig.CONFIG_FILE);
            config.set("configVersion", BlockActivity.LATEST_VERSION);
            try {
                config.save(ActivityConfig.CONFIG_FILE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
