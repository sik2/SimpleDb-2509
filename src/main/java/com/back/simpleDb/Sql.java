package com.back.simpleDb;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class Sql {
    private final SimpleDb simpleDb;
    private final StringBuilder sqlBuilder = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sql, Object... params) {
        if (sqlBuilder.length() > 0) sqlBuilder.append(" ");
        sqlBuilder.append(sql);
        if (params != null) {
            Collections.addAll(this.params, params);
        }
        return this;
    }

    // ? 하나를 params 개수만큼 ?, ?, ? 로 확장
    public Sql appendIn(String sql, Object... params) {
        int count = params.length;
        String placeholders = String.join(", ", Collections.nCopies(count, "?"));
        String expanded = sql.replaceFirst("\\?", placeholders);
        if (sqlBuilder.length() > 0) sqlBuilder.append(" ");
        sqlBuilder.append(expanded);
        Collections.addAll(this.params, params);
        return this;
    }

    // DDL / DML 실행 (반환값 없음)
    public void run() {
        printDevMode();
        try (PreparedStatement ps = buildPreparedStatement(false)) {
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // INSERT → 생성된 PK 반환
    public long insert() {
        printDevMode();
        try (PreparedStatement ps = buildPreparedStatement(true)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    // UPDATE → 영향받은 row 수 반환
    public int update() {
        printDevMode();
        try (PreparedStatement ps = buildPreparedStatement(false)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // DELETE → 영향받은 row 수 반환
    public int delete() {
        return update();
    }

    // 다건 조회 → List<Map>
    public List<Map<String, Object>> selectRows() {
        printDevMode();
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = buildPreparedStatement(false);
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData rsmd = rs.getMetaData();
            while (rs.next()) {
                rows.add(mapRow(rs, rsmd));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rows;
    }

    // 다건 조회 → List<T>
    public <T> List<T> selectRows(Class<T> clazz) {
        List<T> list = new ArrayList<>();
        for (Map<String, Object> row : selectRows()) {
            list.add(mapToObject(row, clazz));
        }
        return list;
    }

    // 단건 조회 → Map
    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();
        return rows.isEmpty() ? null : rows.get(0);
    }

    // 단건 조회 → T
    public <T> T selectRow(Class<T> clazz) {
        Map<String, Object> row = selectRow();
        return row == null ? null : mapToObject(row, clazz);
    }

    // 단일 Long 값 조회
    public Long selectLong() {
        printDevMode();
        try (PreparedStatement ps = buildPreparedStatement(false);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    // 다건 Long 값 조회
    public List<Long> selectLongs() {
        printDevMode();
        List<Long> list = new ArrayList<>();
        try (PreparedStatement ps = buildPreparedStatement(false);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getLong(1));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    // 단일 String 값 조회
    public String selectString() {
        printDevMode();
        try (PreparedStatement ps = buildPreparedStatement(false);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    // 단일 Boolean 값 조회
    public Boolean selectBoolean() {
        printDevMode();
        try (PreparedStatement ps = buildPreparedStatement(false);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getBoolean(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    // 단일 LocalDateTime 값 조회
    public LocalDateTime selectDatetime() {
        printDevMode();
        try (PreparedStatement ps = buildPreparedStatement(false);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                // UTC로 읽은 뒤 시스템 타임존(KST)으로 변환
                Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                Timestamp ts = rs.getTimestamp(1, utcCal);
                return ts != null
                        ? ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    // ===== Private helpers =====

    private PreparedStatement buildPreparedStatement(boolean returnGeneratedKeys) throws SQLException {
        Connection conn = simpleDb.getConnection();
        PreparedStatement ps = returnGeneratedKeys
                ? conn.prepareStatement(sqlBuilder.toString(), Statement.RETURN_GENERATED_KEYS)
                : conn.prepareStatement(sqlBuilder.toString());
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
        return ps;
    }

    // ResultSet 한 행을 Map으로 변환
    private Map<String, Object> mapRow(ResultSet rs, ResultSetMetaData rsmd) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        int colCount = rsmd.getColumnCount();
        for (int i = 1; i <= colCount; i++) {
            String colName = rsmd.getColumnLabel(i);
            int colType = rsmd.getColumnType(i);
            Object value;
            switch (colType) {
                case Types.INTEGER:
                case Types.BIGINT:
                case Types.SMALLINT:
                case Types.TINYINT:
                    value = rs.getLong(colName);
                    break;
                case Types.VARCHAR:
                case Types.LONGVARCHAR:
                case Types.CHAR:
                    value = rs.getString(colName);
                    break;
                case Types.TIMESTAMP:
                    value = rs.getObject(colName, LocalDateTime.class);
                    break;
                case Types.BIT:
                case Types.BOOLEAN:
                    value = rs.getBoolean(colName);
                    break;
                case Types.DOUBLE:
                case Types.FLOAT:
                    value = rs.getDouble(colName);
                    break;
                default:
                    value = rs.getObject(colName);
            }
            row.put(colName, value);
        }
        return row;
    }

    // Map → DTO 리플렉션 매핑
    private <T> T mapToObject(Map<String, Object> row, Class<T> clazz) {
        try {
            T obj = clazz.getDeclaredConstructor().newInstance();
            for (Field field : getAllFields(clazz)) {
                field.setAccessible(true);
                String fieldName = field.getName();
                Object value = row.get(fieldName);
                if (value == null) {
                    // 대소문자 무시 폴백
                    for (Map.Entry<String, Object> entry : row.entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(fieldName)) {
                            value = entry.getValue();
                            break;
                        }
                    }
                }
                if (value != null) {
                    field.set(obj, convertValue(value, field.getType()));
                }
            }
            return obj;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isAssignableFrom(value.getClass())) return value;
        if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number) return ((Number) value).longValue();
        }
        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) return ((Number) value).intValue();
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Boolean) return value;
            if (value instanceof Number) return ((Number) value).intValue() != 0;
        }
        if (targetType == String.class) {
            return value.toString();
        }
        return value;
    }

    // devMode일 때 rawSql 출력
    private void printDevMode() {
        if (!simpleDb.isDevMode()) return;
        String rawSql = sqlBuilder.toString();
        for (Object param : params) {
            rawSql = rawSql.replaceFirst("\\?", "'" + param + "'");
        }
        System.out.println("== rawSql ==");
        System.out.println(rawSql);
        System.out.println();
    }
}