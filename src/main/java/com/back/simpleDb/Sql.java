package com.back.simpleDb;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class Sql {
    private final SimpleDb simpleDb;
    private final StringBuilder sqlBuilder = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sql, Object... params) {
        if (!sqlBuilder.isEmpty()) sqlBuilder.append(" ");
        sqlBuilder.append(sql);
        for (Object param : params) {
            this.params.add(param);
        }
        return this;
    }

    public long insert() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString(), Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) return rs.getLong(1);
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int update() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public int delete() { Connection conn = simpleDb.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, Object>> selectRows() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();
        return rows.isEmpty() ? null : rows.get(0);
    }

    public LocalDateTime selectDatetime() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getTimestamp(1).toLocalDateTime();
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Long selectLong() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getLong(1);
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String selectString() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString(1);
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Boolean selectBoolean() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getBoolean(1);
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Long> selectLongs() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            List<Long> list = new ArrayList<>();
            while (rs.next()) list.add(rs.getLong(1));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Sql appendIn(String sql, Object... params) {
        if (!sqlBuilder.isEmpty()) sqlBuilder.append(" ");
        String placeholders = String.join(", ", Collections.nCopies(params.length, "?"));
        String expanded = sql.replace("?", placeholders);
        sqlBuilder.append(expanded);
        for (Object param : params) {
            this.params.add(param);
        }
        return this;
    }

    public <T> List<T> selectRows(Class<T> clazz) {
        List<Map<String, Object>> rows = selectRows();
        List<T> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(mapToObject(row, clazz));
        }
        return result;
    }

    public <T> T selectRow(Class<T> clazz) {
        Map<String, Object> row = selectRow();
        return row == null ? null : mapToObject(row, clazz);
    }

    private <T> T mapToObject(Map<String, Object> row, Class<T> clazz) {
        try {
            T obj = clazz.getDeclaredConstructor().newInstance();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                Field field = findField(clazz, entry.getKey());
                if (field == null) continue;
                field.setAccessible(true);
                field.set(obj, entry.getValue());
            }
            return obj;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Field findField(Class<?> clazz, String colName) {
        for (Field f : clazz.getDeclaredFields()) {
            if (f.getName().equalsIgnoreCase(colName)) return f;
        }
        return null;
    }
}
