package com.back.simpleDb;

import java.sql.*;

public class SimpleDb {

    private final String host;
    private final String user;
    private final String password;
    private final String dbName;
    private boolean devMode = false;

    public SimpleDb(String host, String user, String password, String dbName) {
        this.host = host;
        this.user = user;
        this.password = password;
        this.dbName = dbName;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found", e);
        }
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public boolean isDevMode() {
        return devMode;
    }

    private Connection createConnection() {
        String url = String.format(
                "jdbc:mysql://%s:3306/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul",
                host, dbName
        );
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create connection: " + e.getMessage(), e);
        }
    }
}