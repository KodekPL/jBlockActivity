package jcraft.jblockactivity.sql;

public class SQLProfile {
    private final String user;
    private final String database;
    private final String password;
    private final String port;
    private final String host;

    public SQLProfile(String host, String portnmbr, String database, String username, String password) {
        this.host = host;
        this.port = portnmbr;
        this.database = database;
        this.user = username;
        this.password = password;
    }

    public String getHost() {
        return this.host;
    }

    public String getPort() {
        return this.port;
    }

    public String getDatabase() {
        return this.database;
    }

    public String getUser() {
        return this.user;
    }

    public String getPassword() {
        return this.password;
    }

}
