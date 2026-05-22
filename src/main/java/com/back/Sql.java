package com.back;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class Sql {
    private final SimpleDb simpleDb;
    private final StringBuilder sqlBuilder = new StringBuilder();
    private final List<Object> args = new ArrayList<>();

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sql) {
        sqlBuilder.append(sql).append("\n");
        return this;
    }

    public Sql append(String sql, Object... args) {
        sqlBuilder.append(sql).append("\n");
        this.args.addAll(Arrays.asList(args));
        return this;
    }

    public String getRawSql() {
        return sqlBuilder.toString();
    }

    public String getCompleteSql() {
        return simpleDb.toCompleteSql(getRawSql(), args.toArray());
    }

    private void logIfDevMode() {
        if (simpleDb.isDevMode()) {
            System.out.println("== rawSql ==");
            System.out.println(getCompleteSql());
        }
    }

    private void bindParams(PreparedStatement pstmt) throws SQLException {
        for (int i = 0; i < args.size(); i++) {
            pstmt.setObject(i + 1, args.get(i));
        }
    }

    private long execute(boolean returnGeneratedKeys) {
        logIfDevMode();
        int keyOption = returnGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS;
        try {
            Connection conn = simpleDb.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(getRawSql(), keyOption);
            bindParams(pstmt);
            int affected = pstmt.executeUpdate();
            if (returnGeneratedKeys) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) return rs.getLong(1);
                return -1;
            }
            return affected;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private ResultSet executeQuery() {
        logIfDevMode();
        try {
            Connection conn = simpleDb.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(getRawSql());
            bindParams(pstmt);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Object toJavaType(ResultSet rs, int index, String typeName) throws SQLException {
        return switch (typeName.toUpperCase()) {
            case "DATETIME", "TIMESTAMP" -> {
                Timestamp ts = rs.getTimestamp(index);
                yield ts != null ? ts.toLocalDateTime() : null;
            }
            case "BIT" -> rs.getBoolean(index);
            default -> rs.getObject(index);
        };
    }

    public long insert() { return execute(true); }
    public int update() { return (int) execute(false); }
    public int delete() { return (int) execute(false); }

    public List<Map<String, Object>> selectRows() {
        try {
            ResultSet rs = executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(meta.getColumnName(i), toJavaType(rs, i, meta.getColumnTypeName(i)));
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
        try {
            ResultSet rs = executeQuery();
            if (rs.next()) return rs.getTimestamp(1).toLocalDateTime();
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Long selectLong() {
        try {
            ResultSet rs = executeQuery();
            if (rs.next()) return rs.getLong(1);
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String selectString() {
        try {
            ResultSet rs = executeQuery();
            if (rs.next()) return rs.getString(1);
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Boolean selectBoolean() {
        try {
            ResultSet rs = executeQuery();
            if (rs.next()) return rs.getBoolean(1);
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Sql appendIn(String sql, Object... args){
        return null;
    }

    public <T> List<T> selectRows(Class<T> clazz) {
        return null;
    }

    public <T> T selectRow(Class<T> clazz) {
        return null;
    }


    public List<Long> selectLongs() {
        return null;
    }
}
