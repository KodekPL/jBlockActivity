package jcraft.jblockactivity.utils;

import static org.bukkit.Bukkit.getLogger;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import jcraft.jblockactivity.BlockActivity;
import jcraft.jblockactivity.config.ActivityConfig;

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
            getLogger().log(Level.INFO, "[jBlockActivity] SQLUpdater started updating to config version 2, that can take a while...");
            final Connection connection = BlockActivity.getBlockActivity().getConnection();
            try {
                connection.setAutoCommit(false);
                final Statement state1 = connection.createStatement();
                getLogger().log(Level.INFO, "[jBlockActivity] Updating ba-players table to save players UUID...");
                state1.execute("ALTER TABLE `ba-players` ADD uuid VARCHAR(32) UNIQUE;");
                connection.commit();
                state1.close();

                final Statement state2 = connection.createStatement();
                getLogger().log(Level.INFO, "[jBlockActivity] Fetching all player names from database...");
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
                getLogger().log(Level.INFO, "[jBlockActivity] Fetched " + playerIds.size() + " names from database.");

                final ProfileRepository repo = new HttpProfileRepository("minecraft");
                getLogger().log(Level.INFO, "[jBlockActivity] Fetching players UUID from Mojang database...");
                final Profile[] profiles = repo.findProfilesByNames(playerIds.keySet().toArray(new String[playerIds.size()]));

                final Statement state3 = connection.createStatement();
                for (Profile profile : profiles) {
                    if (profile == null || !playerIds.containsKey(profile.getName())) {
                        continue;
                    }
                    int playerId = playerIds.get(profile.getName());
                    state3.executeUpdate("UPDATE `ba-players` SET `uuid` = '" + profile.getId() + "' WHERE `playerid` = " + playerId);
                    getLogger()
                            .log(Level.INFO,
                                    "[jBlockActivity] Player " + profile.getName() + "(" + playerId + ") has been updated to UUID ("
                                            + profile.getId() + ").");
                    playerIds.remove(profile.getName());
                }
                connection.commit();

                for (String name : playerIds.keySet()) {
                    getLogger().log(Level.WARNING, "[jBlockActivity] Could not find UUID for player " + name + ".");
                }

                state3.close();
                connection.close();

                getLogger().log(Level.INFO, "[jBlockActivity] Converting players to UUID system has been completed.");
            } catch (final SQLException e) {
                getLogger().log(Level.SEVERE, "[SQLUpdater] Error: ", e);
                return;
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
