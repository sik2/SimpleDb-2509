package com.back;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class Sql {
    private final SimpleDb simpleDb;

    private final StringBuilder sqlBuilder =
            new StringBuilder();

    private final List<Object> args =
            new ArrayList<>();

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
        return simpleDb.toCompleteSql(getRawSql(), args.toArray()
        );
    }

    private void logIfDevMode() {
        if (simpleDb.isDevMode()) {
            System.out.println("== rawSql ==");
            System.out.println(getCompleteSql());
        }
    }

    public long insert() {
        logIfDevMode();
        String sql = getRawSql();
        try (
                Connection conn = simpleDb.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            bindParams(pstmt);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int update() {
        logIfDevMode();
        String sql = getRawSql();
        try (
                Connection conn = simpleDb.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
        ) {
            bindParams(pstmt);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int delete() {
        logIfDevMode();
        String sql = getRawSql();
        try (
                Connection conn = simpleDb.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
        ) {
            bindParams(pstmt);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, Object>> selectRows() {
        logIfDevMode();
        try {
            Connection conn = simpleDb.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(getRawSql());
            bindParams(pstmt);
            ResultSet rs = pstmt.executeQuery();
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

    private void bindParams(PreparedStatement pstmt) throws SQLException {
        for (int i = 0; i < args.size(); i++) {
            pstmt.setObject(i + 1, args.get(i));
        }
    }

    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();
        if (rows.isEmpty()) return null;
        return rows.get(0);
    }

    public LocalDateTime selectDatetime() {
        logIfDevMode();
        try {
            Connection conn = simpleDb.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(getRawSql());
            bindParams(pstmt);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getTimestamp(1).toLocalDateTime();
            }
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


    public Long selectLong() {
        return null;
    }

    public String selectString() {
        return null;
    }

    public Boolean selectBoolean() {
        return null;
    }

    public List<Long> selectLongs() {
        return null;
    }
}
