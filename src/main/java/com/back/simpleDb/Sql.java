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
        sqlBuilder.append(sql).append(" ");
        for (Object param : params) {
            this.params.add(param);
        }
        return this;
    }

    public Sql appendIn(String sql, Object... params) {
        String questionMarks = "?";
        for (int i = 1; i < params.length; i++) {
            questionMarks += ", ?";
        }
        sql = sql.replace("?", questionMarks);
        sqlBuilder.append(sql).append(" ");
        for (Object param : params) {
            this.params.add(param);
        }
        return this;
    }

    public long insert() {
        String sql = buildSql();
        try (PreparedStatement ps = simpleDb.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindParams(ps);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int executeUpdate() {
        String sql = buildSql();
        try (PreparedStatement ps = simpleDb.getConnection().prepareStatement(sql)) {
            bindParams(ps);
            return ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int update() {
        return executeUpdate();
    }

    public int delete() {
        return executeUpdate();
    }

    public List<Map<String, Object>> selectRows() {
        String sql = buildSql();
        try (PreparedStatement ps = simpleDb.getConnection().prepareStatement(sql)) {
            bindParams(ps);
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                rows.add(row);
            }
            return rows;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> List<T> selectRows(Class<T> clazz) {
        List<Map<String, Object>> rows = selectRows();
        List<T> objects = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            objects.add(mapToObject(row, clazz));
        }
        return objects;
    }

    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();
        if (rows.isEmpty()) {
            return null;
        }
        return rows.get(0);
    }

    public <T> T selectRow(Class<T> clazz) {
        Map<String, Object> row = selectRow();
        if (row == null) {
            return null;
        }
        return mapToObject(row, clazz);
    }

    public LocalDateTime selectDatetime() {
        Map<String, Object> row = selectRow();
        if (row == null) {
            return null;
        }
        return (LocalDateTime) getFirstValue(row);
    }

    public Long selectLong() {
        Map<String, Object> row = selectRow();
        if (row == null) {
            return null;
        }
        return ((Number) getFirstValue(row)).longValue();
    }

    public List<Long> selectLongs() {
        List<Map<String, Object>> rows = selectRows();
        List<Long> longs = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            longs.add(((Number) getFirstValue(row)).longValue());
        }
        return longs;
    }

    public String selectString() {
        Map<String, Object> row = selectRow();
        if (row == null) {
            return null;
        }
        return (String) getFirstValue(row);
    }

    public Boolean selectBoolean() {
        Map<String, Object> row = selectRow();
        if (row == null) {
            return null;
        }
        Object value = getFirstValue(row);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return ((Number) value).intValue() == 1;
    }

    private Object getFirstValue(Map<String, Object> row) {
        return row.values().iterator().next();
    }

    private void bindParams(PreparedStatement ps) throws Exception {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private String buildSql() {
        String sql = sqlBuilder.toString();
        if (simpleDb.isDevMode()) {
            System.out.println("== rawSql ==\n" + sql + "\n");
        }
        return sql;
    }

    private <T> T mapToObject(Map<String, Object> row, Class<T> clazz) {
        try {
            T obj = clazz.getDeclaredConstructor().newInstance();
            String[] columnNames = row.keySet().toArray(new String[0]);
            for (String columnName : columnNames) {
                Object value = row.get(columnName);
                Field field = clazz.getDeclaredField(columnName);
                field.setAccessible(true);
                field.set(obj, value);
            }
            return obj;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
