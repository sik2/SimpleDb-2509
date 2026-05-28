package com.back.simpleDb;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class Sql implements AutoCloseable {

    private final SimpleDb simpleDb;
    private final StringBuilder queryBuilder = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sqlPart, Object... args) {
        if (!queryBuilder.isEmpty()) {
            queryBuilder.append(" ");
        }
        queryBuilder.append(sqlPart);
        if (args != null) {
            params.addAll(Arrays.asList(args));
        }
        return this;
    }

    public Sql appendIn(String sqlPart, Object... args) {
        if (args.length == 1 && args[0] instanceof Long[]) {
            args = (Long[]) args[0];
        } else if (args.length == 1 && args[0] instanceof Object[]) {
            args = (Object[]) args[0];
        }

        StringBuilder inPlaceholders = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            inPlaceholders.append("?");
            if (i < args.length - 1) inPlaceholders.append(", ");
        }

        String processedSql = sqlPart.replace("?", inPlaceholders.toString());
        return this.append(processedSql, args);
    }

    private PreparedStatement prepare() throws SQLException {
        Connection conn = simpleDb.getConnection();
        PreparedStatement stmt = conn.prepareStatement(queryBuilder.toString(), Statement.RETURN_GENERATED_KEYS);

        for (int i = 0; i < params.size(); i++) {
            stmt.setObject(i + 1, params.get(i));
        }
        return stmt;
    }

    // ==========================================
    // 중복 제거를 위한 공통 템플릿 메서드
    // ==========================================
    private <T> T executeQueryAndMap(ResultSetMapper<T> mapper) {
        try (PreparedStatement stmt = prepare(); ResultSet rs = stmt.executeQuery()) {
            return mapper.map(rs);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    private interface ResultSetMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    // ==========================================

    public long insert() {
        try (PreparedStatement stmt = prepare()) {
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

    public int update() {
        try (PreparedStatement stmt = prepare()) {
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int delete() {
        return update();
    }

    public List<Map<String, Object>> selectRows() {
        return executeQueryAndMap(rs -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);

                    if (value instanceof Timestamp) {
                        value = ((Timestamp) value).toLocalDateTime();
                    } else if (value instanceof Boolean || (metaData.getColumnType(i) == Types.BIT)) {
                        value = rs.getBoolean(i);
                    }
                    row.put(columnName, value);
                }
                rows.add(row);
            }
            return rows;
        });
    }

    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();
        return rows.isEmpty() ? null : rows.get(0);
    }

    public <T> List<T> selectRows(Class<T> cls) {
        List<Map<String, Object>> rows = selectRows();
        List<T> result = new ArrayList<>();
        try {
            for (Map<String, Object> row : rows) {
                T obj = cls.getDeclaredConstructor().newInstance();
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    try {
                        Field field = cls.getDeclaredField(entry.getKey());
                        field.setAccessible(true);
                        field.set(obj, entry.getValue());
                    } catch (NoSuchFieldException e) {
                        // DTO 변수명과 매칭되지 않는 컬럼 무시
                    }
                }
                result.add(obj);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public <T> T selectRow(Class<T> cls) {
        List<T> list = selectRows(cls);
        return list.isEmpty() ? null : list.get(0);
    }

    public LocalDateTime selectDatetime() {
        return executeQueryAndMap(rs -> {
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp(1);
                return ts != null ? ts.toLocalDateTime() : null;
            }
            return null;
        });
    }

    public Long selectLong() {
        return executeQueryAndMap(rs -> rs.next() ? rs.getLong(1) : null);
    }

    public List<Long> selectLongs() {
        return executeQueryAndMap(rs -> {
            List<Long> list = new ArrayList<>();
            while (rs.next()) {
                list.add(rs.getLong(1));
            }
            return list;
        });
    }

    public String selectString() {
        return executeQueryAndMap(rs -> rs.next() ? rs.getString(1) : null);
    }

    public Boolean selectBoolean() {
        return executeQueryAndMap(rs -> rs.next() ? rs.getBoolean(1) : null);
    }

    @Override
    public void close() {
        // 자원 해제용  (미구현함)
    }
}