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

    public void close() {}
    public void startTransaction() { throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다."); }
    public void rollback() { throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다."); }
    public void commit() { throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다."); }
}
