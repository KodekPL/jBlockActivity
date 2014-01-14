package jcraft.jblockactivity.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.Bukkit;

public class SQLConnection {
    private final SQLProfile profile;
    private Connection connection;

    public SQLConnection(SQLProfile profile) {
        this.profile = profile;
    }

    public void open() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            this.connection = DriverManager.getConnection(
                    "jdbc:mysql://" + profile.getHost() + ":" + profile.getPort() + "/" + profile.getDatabase(), profile.getUser(),
                    profile.getPassword());
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not connect to MySQL server! Reason: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            Bukkit.getLogger().log(Level.SEVERE, "JDBC Driver not found! " + e);
        }
    }

    public boolean checkConnection() {
        if (this.connection != null) {
            return true;
        }
        return false;
    }

    public Connection getConnection() {
        return this.connection;
    }

    public void closeConnection() throws SQLException {
        this.connection.close();
        this.connection = null;
    }
}
