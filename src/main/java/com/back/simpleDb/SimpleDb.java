package com.back.simpleDb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SimpleDb {
    private final String host;
    private final String username;
    private final String password;
    private final String dbName;
    private boolean devMode;
    private final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();

    public SimpleDb(String host, String username, String password, String dbName) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.dbName = dbName;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public Sql genSql() {
        return new Sql(this);
    }

    public int run(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            bindParams(pstmt, params);
            if (devMode) {
                System.out.println("[SQL] " + sql);
            }
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("SQL 실행 중 오류가 발생했습니다.", e);
        }
    }

    Connection getConnection() throws SQLException {
        Connection txConn = transactionConnection.get();
        if (txConn != null) {
            return txConn;
        }

        return createNewConnection();
    }

    private Connection createNewConnection() throws SQLException {
        String url = "jdbc:mysql://" + host + ":3306/?serverTimezone=Asia/Seoul";
        Connection conn = DriverManager.getConnection(url, username, password);

        try (PreparedStatement createDb = conn.prepareStatement("CREATE DATABASE IF NOT EXISTS `" + dbName + "`")) {
            createDb.executeUpdate();
        }
        try (PreparedStatement useDb = conn.prepareStatement("USE `" + dbName + "`")) {
            useDb.execute();
        }

        return conn;
    }

    static void bindParams(PreparedStatement pstmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            pstmt.setObject(i + 1, params[i]);
        }
    }

    void closeConnection(Connection conn) {
        if (conn == null) return;
        if (transactionConnection.get() == conn) return;
        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException("커넥션 종료 중 오류가 발생했습니다.", e);
        }
    }

    public void close() {
        Connection conn = transactionConnection.get();
        if (conn == null) return;
        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException("트랜잭션 커넥션 종료 중 오류가 발생했습니다.", e);
        } finally {
            transactionConnection.remove();
        }
    }

    public void startTransaction() {
        if (transactionConnection.get() != null) return;
        try {
            Connection conn = createNewConnection();
            conn.setAutoCommit(false);
            transactionConnection.set(conn);
        } catch (SQLException e) {
            throw new RuntimeException("트랜잭션 시작 중 오류가 발생했습니다.", e);
        }
    }

    public void rollback() {
        Connection conn = transactionConnection.get();
        if (conn == null) return;
        try {
            conn.rollback();
        } catch (SQLException e) {
            throw new RuntimeException("롤백 중 오류가 발생했습니다.", e);
        } finally {
            close();
        }
    }

    public void commit() {
        Connection conn = transactionConnection.get();
        if (conn == null) return;
        try {
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("커밋 중 오류가 발생했습니다.", e);
        } finally {
            close();
        }
    }
}
