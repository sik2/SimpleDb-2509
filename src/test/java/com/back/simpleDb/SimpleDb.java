package com.back.simpleDb;

import java.sql.*;

public class SimpleDb {
    private final SimpleDataSource dataSource;
    private final ThreadLocal<Connection> threadLocalConnection = new ThreadLocal<>();

    SimpleDb(String host, String dbUser, String dbPassword, String dbName) {
        String url = "jdbc:mysql://" + host + ":3306/" + dbName + "?serverTimezone=Asia/Seoul" + "&characterEncoding=UTF-8";
        this.dataSource = new SimpleDataSource(url, dbUser, dbPassword);
    }

    public Connection getConnection() throws SQLException {
        Connection connection = threadLocalConnection.get();

        // transaction 중이면 기존 connection 재사용
        if (connection != null && !connection.isClosed()) {
            return connection;
        }

        // transaction 아니면 새 connection 반환
        return dataSource.getConnection();
    }

    public void setDevMode(boolean devMode) {
        if(devMode) {
            ;
        }
    }

    public void run(String sql)  {
        try {
            Statement statement = getConnection().createStatement();
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void run(String sql, Object... params) {
        String result = sql;

        for (Object param : params) {
            String value;

            if (param instanceof String) {
                value = "'" + param + "'";
            } else {
                value = param.toString();
            }

            result = result.replaceFirst("\\?", value);
        }
        this.run(result);
    }

    public Sql genSql() {
        try {
            return new Sql(getConnection());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            getConnection().close();
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    public void startTransaction() {
        try {
            Connection connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            threadLocalConnection.set(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        Connection connection = threadLocalConnection.get();
        if (connection == null) {
            return;
        }

        try {
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeTransactionConnection();
        }
    }

    public void rollback() {
        Connection connection = threadLocalConnection.get();
        if (connection == null) {
            return;
        }

        try {
            connection.rollback();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeTransactionConnection();
        }
    }

    public boolean isTransactionActive() {
        Connection connection = threadLocalConnection.get();
        return connection != null;
    }

    private void closeTransactionConnection() {
        Connection connection = threadLocalConnection.get();
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            threadLocalConnection.remove();
        }
    }
}
