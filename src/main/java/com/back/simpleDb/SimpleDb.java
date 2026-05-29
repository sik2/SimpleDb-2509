package com.back.simpleDb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SimpleDb {


    private final String host;
    private final String user;
    private final String password;
    private final String database;
    private boolean devMode;

    private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    public SimpleDb(String host, String user, String password, String database) {
        this.host = host;
        this.user = user;
        this.password = password;
        this.database = database;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public boolean isDevMode() {
        return devMode;
    }

    Connection getConnection() {
        Connection conn = connectionHolder.get();
        try {
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(
                        "jdbc:mysql://" + host + "/" + database, user, password
                );
                try (PreparedStatement ps = conn.prepareStatement("SET time_zone = '+09:00'")) {
                    ps.execute();
                }
                connectionHolder.set(conn);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return conn;
    }

    public void run(String sql, Object... params) {
        if (devMode) {
            System.out.println("== rawSql ==\n" + sql + "\n");
        }
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Sql genSql() {
        return new Sql(this);
    }

    public void close() {}

    public void startTransaction() {}

    public void rollback() {}

    public void commit() {}
}