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

    public void setDevMode(boolean devMode) { this.devMode = devMode; }
    public boolean isDevMode() { return devMode; }

    Connection getConnection() {
        Connection conn = connectionHolder.get();
        if (conn == null) {
            try {
                String url = "jdbc:mysql://%s:3306/%s?serverTimezone=Asia/Seoul".formatted(host, dbName);
                conn = DriverManager.getConnection(url, user, password);
                connectionHolder.set(conn);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return conn;
    }

    public void close() {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                connectionHolder.remove();
            }
        }
    }

    public void run(String sql, Object... params) {
        Sql s = genSql();
        s.append(sql, params);
        s.execute();
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