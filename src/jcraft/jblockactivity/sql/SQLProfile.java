package jcraft.jblockactivity.sql;

public class SQLProfile {
    private final String user;
    private final String database;
    private final String password;
    private final String port;
    private final String host;

    public SQLProfile(String host, String port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = username;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getUrl() {
        return "jdbc:mysql://" + getHost() + ":" + getPort() + "/" + getDatabase() + "?useUnicode=true&characterEncoding=utf-8";
    }

}
