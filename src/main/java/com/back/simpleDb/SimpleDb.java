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

    public SimpleDb(String host, String user, String password, String dbName) {
        this.host = host;
        this.user = user;
        this.password = password;
        this.dbName = dbName;
    }

    private Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://" + host + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true";
        return DriverManager.getConnection(url, user, password);
    }

    public void run(String query, Object... params) {
        if (devMode) System.out.println("[SQL] " + query);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

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
}
