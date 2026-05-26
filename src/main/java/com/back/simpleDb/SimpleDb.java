package com.back.simpleDb;


import lombok.Getter;

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
    private final ThreadLocal<Connection> myConn = new ThreadLocal<>();

    public SimpleDb(String host, String username, String password, String database) {
        this.host = host;
        this.user = username;
        this.password = password;
        this.database = database;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    private Connection getConnection() {
        Connection conn = myConn.get();
        try {
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(
                        "jdbc:mysql://" + host + "/" + database, user, password
                );
                myConn.set(conn);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return conn;
    }

    public void run(String sql, Object... params) {
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
        return new Sql();
    }

    public void startTransaction() {
    }

    public void rollback() {
    }

    public void commit() {
    }

    public void close() {
    }
}