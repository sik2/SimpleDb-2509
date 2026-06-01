package com.back.simpleDb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SimpleDb {
    private final String url;
    private final String serverUrl;
    private final String username;
    private final String password;
    private final String database;
    private boolean devMode;
    private final ThreadLocal<Connection> connections = new ThreadLocal<>();
    private final ThreadLocal<Boolean> inTransaction = ThreadLocal.withInitial(() -> false);

    public SimpleDb(String host, String username, String password, String database) {
        String options = "serverTimezone=Asia/Seoul&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true";
        this.url = "jdbc:mysql://%s:3306/%s?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true"
                .formatted(host, database);
        this.serverUrl = "jdbc:mysql://%s:3306/?%s".formatted(host, options);
        this.username = username;
        this.password = password;
        this.database = database;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public boolean isDevMode() {
        return devMode;
    }

    public Sql genSql() {
        return new Sql(this);
    }

    public int run(String sql, Object... params) {
        String trimmedSql = sql.trim();

        if (params.length == 0 && trimmedSql.matches("(?i)^TRUNCATE\\s+(TABLE\\s+)?`?[a-zA-Z_][a-zA-Z0-9_]*`?$")) {
            String tableName = trimmedSql
                    .replaceFirst("(?i)^TRUNCATE\\s+", "")
                    .replaceFirst("(?i)^TABLE\\s+", "");
            int affectedRows = genSql().append("DELETE FROM " + tableName).update();
            genSql().append("ALTER TABLE " + tableName + " AUTO_INCREMENT = 1").update();
            return affectedRows;
        }

        return genSql().append(sql, params).update();
    }

    Connection getConnection() {
        Connection connection = connections.get();

        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                connection = openConnection();
                connections.set(connection);
            }

            return connection;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Connection openConnection() throws SQLException {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            if (!isUnknownDatabaseError(e)) {
                throw e;
            }

            createDatabaseIfNotExists();
            return DriverManager.getConnection(url, username, password);
        }
    }

    private boolean isUnknownDatabaseError(SQLException e) {
        return "42000".equals(e.getSQLState()) && e.getMessage() != null && e.getMessage().contains("Unknown database");
    }

    private void createDatabaseIfNotExists() throws SQLException {
        try (Connection connection = DriverManager.getConnection(serverUrl, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `%s` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
                    .formatted(database.replace("`", "``")));
        }
    }

    boolean isInTransaction() {
        return inTransaction.get();
    }

    public void startTransaction() {
        try {
            Connection connection = getConnection();
            connection.setAutoCommit(false);
            inTransaction.set(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        try {
            Connection connection = getConnection();
            connection.commit();
            connection.setAutoCommit(true);
            inTransaction.set(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void rollback() {
        try {
            Connection connection = getConnection();
            connection.rollback();
            connection.setAutoCommit(true);
            inTransaction.set(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        Connection connection = connections.get();

        if (connection == null) {
            return;
        }

        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            connections.remove();
            inTransaction.remove();
        }
    }
}
