package com.back.simpleDb;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.LinkedHashMap;

public class Sql {

    private final SimpleDb simpleDb;
    private final StringBuilder sqlBuilder = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sql, Object... bindParams) {
        if (sqlBuilder.length() > 0) sqlBuilder.append("\n");
        sqlBuilder.append(sql.trim());
        Collections.addAll(params, bindParams);
        return this;
    }

    private String buildSql() {
        return sqlBuilder.toString();
    }

    private void bindParams(PreparedStatement pstmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            pstmt.setObject(i + 1, params.get(i));
        }
    }

    public long insert() {
        String sql = buildSql();
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindParams(pstmt, params);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("INSERT failed: " + e.getMessage(), e);
        }
        return 0L;
    }

    public int update() {
        String sql = buildSql();
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            bindParams(pstmt, params);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("UPDATE failed: " + e.getMessage(), e);
        }
    }
    public int delete() {
        String sql = buildSql();
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            bindParams(pstmt, params);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("DELETE failed: " + e.getMessage(), e);
        }
    }
    public List<Map<String, Object>> selectRows() {
        String sql = buildSql();
        List<Map<String, Object>> result = new ArrayList<>();
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            bindParams(pstmt, params);
            try (ResultSet rs = pstmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(meta.getColumnLabel(i), extractValue(rs, i, meta));
                    }
                    result.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("selectRows failed: " + e.getMessage(), e);
        }
        return result;
    }

    private Object extractValue(ResultSet rs, int index, ResultSetMetaData meta) throws SQLException {
        int sqlType = meta.getColumnType(index);
        return switch (sqlType) {
            case Types.TIMESTAMP -> {
                Timestamp ts = rs.getTimestamp(index);
                yield ts != null ? ts.toLocalDateTime() : null;
            }
            case Types.BIT -> rs.getBoolean(index);
            case Types.INTEGER -> {
                if (!meta.isSigned(index)) yield rs.getLong(index);
                yield rs.getInt(index);
            }
            case Types.BIGINT -> rs.getLong(index);
            default -> rs.getObject(index);
        };
    }
    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();
        return rows.isEmpty() ? null : rows.get(0);
    }
    public LocalDateTime selectDatetime() {
        String sql = buildSql();
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            bindParams(pstmt, params);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp(1);
                    return ts != null ? ts.toLocalDateTime() : null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("selectDatetime failed: " + e.getMessage(), e);
        }
        return null;
    }
    public Long selectLong() {
        String sql = buildSql();
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            bindParams(pstmt, params);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long val = rs.getLong(1);
                    return rs.wasNull() ? null : val;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("selectLong failed: " + e.getMessage(), e);
        }
        return null;
    }
    public List<Long> selectLongs() {
        String sql = buildSql();
        List<Long> result = new ArrayList<>();
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            bindParams(pstmt, params);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) result.add(rs.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("selectLongs failed: " + e.getMessage(), e);
        }
        return result;
    }

    public String selectString() {
        String sql = buildSql();
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            bindParams(pstmt, params);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("selectString failed: " + e.getMessage(), e);
        }
        return null;
    }
    public Boolean selectBoolean() {
        String sql = buildSql();
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            bindParams(pstmt, params);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getBoolean(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("selectBoolean failed: " + e.getMessage(), e);
        }
        return null;
    }
    public Sql appendIn(String sql, Object... values) {
        if (sqlBuilder.length() > 0) sqlBuilder.append("\n");
        String placeholders = String.join(", ", Collections.nCopies(values.length, "?"));
        String expanded = sql.trim().replaceFirst("\\?", placeholders);
        sqlBuilder.append(expanded);
        Collections.addAll(params, values);
        return this;
    }
    public <T> T selectRow(Class<T> clazz) { throw new UnsupportedOperationException(); }
    public <T> List<T> selectRows(Class<T> clazz) { throw new UnsupportedOperationException(); }
}