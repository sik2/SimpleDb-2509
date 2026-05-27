package com.back.simpleDb;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sql {
    private final SimpleDb simpleDb;
    private final StringBuilder sqlBuilder = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sqlPart, Object... args) {
        if (!sqlBuilder.isEmpty()) {
            sqlBuilder.append("\n");
        }
        sqlBuilder.append(sqlPart);
        for (Object arg : args) {
            params.add(arg);
        }
        return this;
    }

    public long insert() {
        String sql = sqlBuilder.toString();

        try (var conn = simpleDb.getConnection();
             var pstmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            SimpleDb.bindParams(pstmt, params.toArray());
            pstmt.executeUpdate();

            try (var rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

            return 0L;
        } catch (Exception e) {
            throw new RuntimeException("INSERT 실행 중 오류가 발생했습니다.", e);
        }
    }

    public Sql appendIn(String sqlPart, Object... args) { throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다."); }
    public int update() { return executeUpdateLike("UPDATE"); }
    public int delete() { return executeUpdateLike("DELETE"); }
    public List<Map<String, Object>> selectRows() {
        String sql = sqlBuilder.toString();
        try (var conn = simpleDb.getConnection();
             var pstmt = conn.prepareStatement(sql)) {
            SimpleDb.bindParams(pstmt, params.toArray());
            try (var rs = pstmt.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(toRowMap(rs));
                }
                return rows;
            }
        } catch (Exception e) {
            throw new RuntimeException("SELECT 실행 중 오류가 발생했습니다.", e);
        }
    }

    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();
        if (rows.isEmpty()) {
            return new HashMap<>();
        }
        return rows.get(0);
    }
    public LocalDateTime selectDatetime() {
        String sql = sqlBuilder.toString();
        try (var conn = simpleDb.getConnection();
             var pstmt = conn.prepareStatement(sql)) {
            SimpleDb.bindParams(pstmt, params.toArray());
            try (var rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    var timestamp = rs.getTimestamp(1);
                    return timestamp == null ? null : timestamp.toLocalDateTime();
                }
            }
            throw new RuntimeException("날짜 조회 결과가 없습니다.");
        } catch (Exception e) {
            throw new RuntimeException("날짜 조회 중 오류가 발생했습니다.", e);
        }
    }

    public Long selectLong() {
        String sql = sqlBuilder.toString();
        try (var conn = simpleDb.getConnection();
             var pstmt = conn.prepareStatement(sql)) {
            SimpleDb.bindParams(pstmt, params.toArray());
            try (var rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Object value = rs.getObject(1);
                if (value == null) {
                    return null;
                }
                return ((Number) value).longValue();
            }
        } catch (Exception e) {
            throw new RuntimeException("LONG 조회 중 오류가 발생했습니다.", e);
        }
    }
    public String selectString() { throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다."); }
    public Boolean selectBoolean() { throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다."); }
    public List<Long> selectLongs() { throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다."); }
    public <T> List<T> selectRows(Class<T> clazz) { throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다."); }
    public <T> T selectRow(Class<T> clazz) { throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다."); }

    private int executeUpdateLike(String actionName) {
        String sql = sqlBuilder.toString();
        try (var conn = simpleDb.getConnection();
             var pstmt = conn.prepareStatement(sql)) {
            SimpleDb.bindParams(pstmt, params.toArray());
            return pstmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(actionName + " 실행 중 오류가 발생했습니다.", e);
        }
    }

    private Map<String, Object> toRowMap(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> row = new HashMap<>();
        var metaData = rs.getMetaData();

        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnLabel(i);
            Object value = rs.getObject(i);

            if (value instanceof java.sql.Timestamp timestamp) {
                value = timestamp.toLocalDateTime();
            } else if (value instanceof byte[] bytes) {
                value = bytes.length > 0 && bytes[0] != 0;
            }

            row.put(columnName, value);
        }

        return row;
    }
}
