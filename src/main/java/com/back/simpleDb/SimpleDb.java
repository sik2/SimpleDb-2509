package com.back.simpleDb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SimpleDb {
    private String host;
    private String user;
    private String password;
    private String dbName;
    private boolean devMode;
    private final ThreadLocal<Connection> threadLocalConnection = new ThreadLocal<>();

    public SimpleDb(String host, String user, String password, String dbName) {
        this.host = host;
        this.user = user;
        this.password = password;
        this.dbName = dbName;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public void run(String sql, Object... params) {
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            pstmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Connection getConnection() {
        Connection conn = threadLocalConnection.get();

        if (conn == null) {
            try {
                conn = DriverManager.getConnection(
                        "jdbc:mysql://" + host + "/" + dbName, user, password
                );
                threadLocalConnection.set(conn);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return conn;
    }

    public Sql genSql() {
        return new Sql(this);
    }

    public void close() {
        Connection conn = threadLocalConnection.get();
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                threadLocalConnection.remove();
            }
        }
    }

    public void startTransaction() {
        try {
            getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void rollback() {
        try {
            Connection conn = getConnection();
            conn.rollback();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        try {
            Connection conn = getConnection();
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
