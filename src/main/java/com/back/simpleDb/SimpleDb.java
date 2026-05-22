package com.back.simpleDb;

import lombok.Getter;
import lombok.Setter;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SimpleDb {
    private final String host;
    private final String username;
    private final String password;
    private final String databaseName;
    private boolean devMode = false;

    public SimpleDb(String host, String username, String password, String databaseName) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.databaseName = databaseName;
    }

    public Sql genSql() {
        return new Sql(this);
    }

    public void run(String sql, Object... params) {
        String url = "jdbc:mysql://" + host + ":3306/" +databaseName;
        try (
            Connection conn = DriverManager.getConnection(url, username, password);
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            for (int i = 1; i <= params.length; ++i)
            {
                stmt.setObject(i, params[i-1]);
            }

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public long insert(String sql, Object... params) {
        String url = "jdbc:mysql://" + host + ":3306/" + databaseName;

        try (
                Connection conn = DriverManager.getConnection(url, username, password);
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

            return 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int runForRowsCount(String sql, Object... params) {
        String url = "jdbc:mysql://" + host + ":3306/" + databaseName;

        try (
                Connection conn = DriverManager.getConnection(url, username, password);
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, Object>> runForRows(String sql, Object... params) {
        String url = "jdbc:mysql://" + host + ":3306/" + databaseName;

        try (
                Connection conn = DriverManager.getConnection(url, username, password);
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);

                        row.put(columnName, value);
                    }

                    rows.add(row);
                }

                return rows;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        System.out.println();
    }

    public void startTransaction() {
    }

    public void rollback() {
    }

    public void commit() {
    }
}
