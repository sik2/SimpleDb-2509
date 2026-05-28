package com.back.simpleDb;

import lombok.Setter;

import java.sql.*;

public class SimpleDb {
    private final String host;
    private final String username;
    private final String password;
    private final String databaseName;
    @Setter
    private boolean devMode = false;

    private final ThreadLocal<Connection> connectionThread = new ThreadLocal<>();

    public SimpleDb(String host, String username, String password, String databaseName) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.databaseName = databaseName;
    }

    Connection getConnection() {
        Connection conn = connectionThread.get();
        try {
            if (conn == null || conn.isClosed()) {
                String url = "jdbc:mysql://" + host + ":3306/" + databaseName;
                conn = DriverManager.getConnection(url, username, password);
                connectionThread.set(conn);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return conn;
    }

    public Sql genSql() {
        return new Sql(this);
    }

    @FunctionalInterface
    interface StatementHandler<T> {
        T handle(PreparedStatement stmt) throws SQLException;
    }

    private void setParams(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }

    <T> T execute(
            String sql,
            Object[] params,
            StatementHandler<T> handler
    ) {
        Connection conn = getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParams(stmt, params);

            return handler.handle(stmt);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T executeWithGeneratedKeys(
            String sql,
            Object[] params,
            StatementHandler<T> handler
    ) {
        Connection conn = getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParams(stmt, params);

            return handler.handle(stmt);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void run(String sql, Object... params) {
        execute(sql, params, PreparedStatement::executeUpdate);
    }

    public long insert(String sql, Object... params) {
        return executeWithGeneratedKeys(sql, params, stmt -> {
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

            return 0L;
        });
    }

    public int runForRowsCount(String sql, Object... params) {
        return execute(sql, params, PreparedStatement::executeUpdate);
    }

    public void close() {
        Connection conn = connectionThread.get();
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                connectionThread.remove();
            }
        }
    }

    public void startTransaction() {
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
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
        } finally {
            close();
        }
    }

    public void commit() {
        try {
            Connection conn = getConnection();
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            close();
        }
    }
}
