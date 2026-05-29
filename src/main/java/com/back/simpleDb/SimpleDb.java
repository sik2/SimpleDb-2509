package com.back.simpleDb;

import lombok.Getter;
import lombok.Setter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;


@Getter
@Setter
public class SimpleDb {
    private final ThreadLocal<Connection> connectionThreadLocal = new ThreadLocal<>();
    private final String serverName;
    private final String username;
    private final String password;
    private final String schemaName;
    private boolean devMode = false;


    public SimpleDb(String serverName, String username, String password, String schemaName) {
        this.serverName = serverName;
        this.username = username;
        this.password = password;
        this.schemaName = schemaName;
    }

    public String run(String sql, Object... args) {
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                pstmt.setObject(i + 1, args[i]);
            }
            pstmt.executeUpdate();
            return sql;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Connection getConnection() {
        try {
            Connection conn = connectionThreadLocal.get();

            if(conn == null || conn.isClosed()) {
                String url = "jdbc:mysql://%s:3306/%s?serverTimezone=UTC".formatted(serverName, schemaName);
                conn = DriverManager.getConnection(url, username, password);
                connectionThreadLocal.set(conn);
            }

            return conn;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Sql genSql() {
        return new Sql(this);
    }


    public void close() {

        try {
            Connection conn = connectionThreadLocal.get();

            if(conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }


    // 트랜잭션 관련 메서드
    public void startTransaction() {
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void rollback() {
        try {
            Connection conn = getConnection();
            conn.rollback();
            conn.setAutoCommit(true);
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        try {
            Connection conn = getConnection();
            conn.commit();
            conn.setAutoCommit(true);
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
