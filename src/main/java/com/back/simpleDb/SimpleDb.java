package com.back.simpleDb;

import lombok.Setter;

import java.sql.*;
import java.util.*;

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
    private interface StatementHandler<T> {
        T handle(PreparedStatement stmt) throws SQLException;
    }

    private void setParams(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }

    private <T> T execute(
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

    public List<Map<String, Object>> runForRows(String sql, Object... params) {
        return execute(sql, params, stmt -> {
            try (ResultSet rs = stmt.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();

                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnName(i), rs.getObject(i));
                    }

                    rows.add(row);
                }

                return rows;
            }
        });
    }

    public <T> List<T> runForRows(String sql, Class<T> cls, Object... params) {
        return execute(sql, params, stmt -> {
            try (ResultSet rs = stmt.executeQuery()) {
                List<T> rows = new ArrayList<>();

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    T instance = cls.getDeclaredConstructor().newInstance();

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);

                        try {
                            var field = cls.getDeclaredField(columnName);
                            field.setAccessible(true);
                            field.set(instance, value);
                        } catch (NoSuchFieldException ignored) {
                        }
                    }

                    rows.add(instance);
                }

                return rows;
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <T> T selectOne(String sql, Class<T> cls, Object... params) {
        return execute(sql, params, stmt -> {
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;

                if (cls == Boolean.class) {
                    Object value = rs.getObject(1);

                    if (value instanceof Boolean bool) {
                        return cls.cast(bool);
                    }

                    if (value instanceof Number number) {
                        return cls.cast(number.intValue() == 1);
                    }
                }

                return cls.cast(rs.getObject(1));
            }
        });
    }

    public <T> List<T> selectOneList(String sql, Class<T> cls, Object... params) {
        return execute(sql, params, stmt -> {
            try (ResultSet rs = stmt.executeQuery()) {
                List<T> list = new ArrayList<>();

                while (rs.next()) {
                    list.add(cls.cast(rs.getObject(1)));
                }

                return list;
            }
        });
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
