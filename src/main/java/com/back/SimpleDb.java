package com.back.simpleDb;

import java.sql.Connection;
import java.sql.DriverManager;
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

    boolean isDevMode() {
        return devMode;
    }

    Connection getConnection() {
        Connection conn = connectionHolder.get();
        if (conn == null) {
            try {
                String url = "jdbc:mysql://" + host + "/" + database
                        + "?serverTimezone=Asia/Seoul&characterEncoding=UTF-8";
                conn = DriverManager.getConnection(url, user, password);
                connectionHolder.set(conn);
            } catch (SQLException e) {
                throw new RuntimeException("DB 연결 실패: " + e.getMessage(), e);
            }
        }
        return conn;
    }

    public void run(String sql, Object... params) {
        Sql sqlObj = genSql();
        sqlObj.append(sql, params);
        sqlObj.execute();
    }

    public Sql genSql() {
        return new Sql(this);
    }

    public void startTransaction() {
        try {
            getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException("트랜잭션 시작 실패", e);
        }
    }

    public void commit() {
        try {
            Connection conn = getConnection();
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("커밋 실패", e);
        }
    }

    public void rollback() {
        try {
            Connection conn = getConnection();
            conn.rollback();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("롤백 실패", e);
        }
    }

    public void close() {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new RuntimeException("커넥션 종료 실패", e);
            } finally {
                connectionHolder.remove();
            }
        }
    }
}

