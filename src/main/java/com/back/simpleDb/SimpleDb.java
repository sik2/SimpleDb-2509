package com.back.simpleDb;

import lombok.Setter;

import java.sql.*;

public class SimpleDb {
    private final String host;
    private final String user;
    private final String password;
    private final String dbName;
    @Setter
    private boolean devMode = false;

    private final ThreadLocal<Connection> connection = new ThreadLocal<>();

    public SimpleDb(String host, String user, String password, String dbName) {
        this.host = host;
        this.user = user;
        this.password = password;
        this.dbName = dbName;
    }

    private Connection getConnection() throws SQLException {
        if (connection.get() == null) {
            String url = "jdbc:mysql://" + host + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true";
            connection.set(DriverManager.getConnection(url, user, password));
        }
        return connection.get();
    }

    public void run(String query, Object... params) {
        if (devMode) System.out.println("[SQL] " + query);

        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.execute();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Sql genSql() {
        try{
            return new Sql(getConnection());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            if (connection.get() != null) {
                connection.get().close();
                connection.remove(); // 스레드 로컬에서 제거
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void startTransaction() {
        try {
            getConnection().setAutoCommit(false);
            getConnection().setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void rollback() {
        try {
            getConnection().rollback();
            getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
