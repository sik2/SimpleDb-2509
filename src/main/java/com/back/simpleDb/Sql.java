package com.back.simpleDb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Sql {
    private final Connection connection;
    private final StringBuilder query;
    private final List<Object> params;

    public Sql(Connection connection) {
        this.connection = connection;
        this.query = new StringBuilder();
        this.params = new ArrayList<>();
    }

    public Sql append(String sql, Object... args) {
        query.append(" ").append(sql);
        Collections.addAll(params, args);
        return this;
    }

    public long insert() {
        try (PreparedStatement ps = connection.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ps.execute();
            var rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("키가 생성되지 않았습니다.");
    }

    public int update() {
        try (PreparedStatement ps = connection.prepareStatement(query.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return ps.executeUpdate();
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int delete() {
        return update();
    }

    public List<Map<String, Object>> selectRows() {
        try (PreparedStatement ps = connection.prepareStatement(query.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            var rs = ps.executeQuery();
            var meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new java.util.HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
            return rows;
        } catch(SQLException e) {
            throw new RuntimeException(e);

        }
    }

    public Map<String, Object> selectRow() {
        try (PreparedStatement ps = connection.prepareStatement(query.toString())){
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            var rs = ps.executeQuery();
            var meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            Map<String, Object> row = new java.util.HashMap<>();
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
            }
            return row;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
