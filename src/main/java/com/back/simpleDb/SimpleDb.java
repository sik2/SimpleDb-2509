package com.back.simpleDb;

import java.sql.*;

public class SimpleDb {
    private final String host;
    private final String user;
    private final String password;
    private final String dbName;
    private boolean devMode = false;

    private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    public SimpleDb(String host, String user, String password, String dbName) {
        this.host = host;
        this.user = user;
        this.password = password;
        this.dbName = dbName;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    boolean isDevMode() {
        return devMode;
    }

    Connection getConnection() {
        Connection conn = connectionHolder.get();
        try {
            if (conn == null || conn.isClosed()) {
                String url = "jdbc:mysql://" + host + "/" + dbName
                        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul";
                conn = DriverManager.getConnection(url, user, password);
                connectionHolder.set(conn);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return conn;
    }

    public void run(String sql, Object... params) {
        new Sql(this).append(sql, params).run();
    }

    public Sql genSql() {
        return new Sql(this);
    }

    public void startTransaction() {
        try {
            getConnection().setAutoCommit(false);
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

    public void rollback() {
        try {
            Connection conn = getConnection();
            conn.rollback();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                connectionHolder.remove();
            }
        }
    }
}