package com.back.simpleDb;

import java.lang.reflect.Method;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Sql {
    private final SimpleDb simpleDb;
    private final StringBuilder sqlBuilder = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sql, Object... params) {
        if (!sqlBuilder.isEmpty()) {
            sqlBuilder.append("\n");
        }

        sqlBuilder.append(sql);

        for (Object param : params) {
            this.params.add(param);
        }

        return this;
    }

    public Sql appendIn(String sql, Object... params) {
        List<Object> flattenedParams = flatten(params);
        String placeholders = String.join(", ", flattenedParams.stream().map(param -> "?").toList());

        append(sql.replaceFirst("\\?", placeholders), flattenedParams.toArray());

        return this;
    }

    public long insert() {
        try (PreparedStatement statement = prepareStatement(true)) {
            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                return 0;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }

            return affectedRows;
        } catch (SQLException e) {
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
        try (PreparedStatement statement = prepareStatement(false);
             ResultSet resultSet = statement.executeQuery()) {
            List<Map<String, Object>> rows = new ArrayList<>();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                Map<String, Object> row = new LinkedHashMap<>();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    row.put(columnName, normalizeValue(resultSet.getObject(i)));
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

        if (rows.isEmpty()) {
            return null;
        }

        return rows.getFirst();
    }

    public <T> List<T> selectRows(Class<T> cls) {
        return selectRows().stream()
                .map(row -> mapToObject(row, cls))
                .toList();
    }

    public <T> T selectRow(Class<T> cls) {
        Map<String, Object> row = selectRow();

        if (row == null) {
            return null;
        }

        return mapToObject(row, cls);
    }

    public LocalDateTime selectDatetime() {
        Object value = selectScalar();

        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }

        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }

        return (LocalDateTime) value;
    }

    public Long selectLong() {
        Object value = selectScalar();

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.parseLong(value.toString());
    }

    public List<Long> selectLongs() {
        return selectRows().stream()
                .map(row -> row.values().iterator().next())
                .map(value -> value instanceof Number number ? number.longValue() : Long.parseLong(value.toString()))
                .toList();
    }

    public String selectString() {
        Object value = selectScalar();

        if (value == null) {
            return null;
        }

        return value.toString();
    }

    public Boolean selectBoolean() {
        Object value = selectScalar();

        if (value == null) {
            return null;
        }

        if (value instanceof Boolean bool) {
            return bool;
        }

        if (value instanceof Number number) {
            return number.intValue() != 0;
        }

        if (value instanceof byte[] bytes) {
            return bytes.length > 0 && bytes[0] != 0;
        }

        return Boolean.parseBoolean(value.toString());
    }

    private int executeUpdate() {
        try (PreparedStatement statement = prepareStatement(false)) {
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Object selectScalar() {
        Map<String, Object> row = selectRow();

        if (row == null || row.isEmpty()) {
            return null;
        }

        return row.values().iterator().next();
    }

    private PreparedStatement prepareStatement(boolean returnGeneratedKeys) throws SQLException {
        PreparedStatement statement = simpleDb.getConnection().prepareStatement(
                sqlBuilder.toString(),
                returnGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS
        );

        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
        }

        return statement;
    }

    private Object normalizeValue(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }

        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }

        if (value instanceof byte[] bytes && bytes.length == 1) {
            return bytes[0] != 0;
        }

        return value;
    }

    private <T> T mapToObject(Map<String, Object> row, Class<T> cls) {
        try {
            T instance = cls.getDeclaredConstructor().newInstance();

            for (Map.Entry<String, Object> entry : row.entrySet()) {
                Method setter = findSetter(cls, entry.getKey());

                if (setter == null) {
                    continue;
                }

                setter.invoke(instance, convertValue(entry.getValue(), setter.getParameterTypes()[0]));
            }

            return instance;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private Method findSetter(Class<?> cls, String columnName) {
        String setterName = "set" + Character.toUpperCase(columnName.charAt(0)) + columnName.substring(1);

        if (columnName.startsWith("is") && columnName.length() > 2 && Character.isUpperCase(columnName.charAt(2))) {
            setterName = "set" + columnName.substring(2);
        }

        for (Method method : cls.getMethods()) {
            if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                return method;
            }
        }

        return null;
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        if (targetType == Long.class || targetType == long.class) {
            return ((Number) value).longValue();
        }

        if (targetType == Integer.class || targetType == int.class) {
            return ((Number) value).intValue();
        }

        if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Boolean bool) {
                return bool;
            }

            if (value instanceof Number number) {
                return number.intValue() != 0;
            }
        }

        return value;
    }

    private List<Object> flatten(Object[] params) {
        List<Object> result = new ArrayList<>();

        for (Object param : params) {
            if (param instanceof Object[] array) {
                result.addAll(List.of(array));
            } else {
                result.add(param);
            }
        }

        return result;
    }
}
