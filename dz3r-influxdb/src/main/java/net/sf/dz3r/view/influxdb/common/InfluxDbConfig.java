package net.sf.dz3r.view.influxdb.common;

public class InfluxDbConfig {
    public final String dbName;
    public final String instance;
    public final String dbURL;
    public final String username;
    public final String password;

    public InfluxDbConfig(String dbName, String instance, String dbURL, String username, String password) {
        this.dbName = dbName;
        this.instance = instance;
        this.dbURL = dbURL;
        this.username = username;
        this.password = password;
    }
}
