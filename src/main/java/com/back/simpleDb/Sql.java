package com.back.simpleDb;

import java.time.LocalDateTime;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Sql {
    private final SimpleDb simpleDb;
    private String sql = "";
    private final List<Object> params = new ArrayList<>();

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sql, Object... params) {
        this.sql += sql + " ";
        if (params.length != 0)
            this.params.addAll(Arrays.asList(params));

        return this;
    }

    public Sql appendIn(String sql, Object... params) {
        String placeholders = String.join(", ", java.util.Collections.nCopies(params.length, "?"));

        this.sql += sql.replace("?", placeholders) + " ";
        this.params.addAll(Arrays.asList(params));

        return this;
    }

    public long insert() {
        return simpleDb.insert(sql, params.toArray());
    }

    public int update() {
        return simpleDb.runForRowsCount(sql, params.toArray());
    }

    public int delete() {
        return simpleDb.runForRowsCount(sql, params.toArray());
    }

    public List<Map<String, Object>> selectRows() {
        return simpleDb.execute(sql, params.toArray(), stmt -> {
            try (ResultSet rs = stmt.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();

                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnName(i), rs.getObject(i));
                    }

                    rows.add(row);
                }

                return rows;
            }
        });
    }

    public Map<String, Object> selectRow() {
        return selectRows().getFirst();
    }

    public <T> List<T> selectRows(Class<T> cls) {
        return simpleDb.execute(sql, params.toArray(), stmt -> {
            try (ResultSet rs = stmt.executeQuery()) {
                List<T> rows = new ArrayList<>();

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    T instance = cls.getDeclaredConstructor().newInstance();

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);

                        try {
                            var field = cls.getDeclaredField(columnName);
                            field.setAccessible(true);
                            field.set(instance, value);
                        } catch (NoSuchFieldException ignored) {
                        }
                    }

                    rows.add(instance);
                }

                return rows;
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <T> T selectRow(Class<T> cls)
    {
        return selectRows(cls).getFirst();
    }

    public LocalDateTime selectDatetime() {
        return selectOne(LocalDateTime.class);
    }

    public Long selectLong() {
        return selectOne(Long.class);
    }

    public List<Long> selectLongs() {
        return selectOneList(Long.class);
    }

    public String selectString() {
        return selectOne(String.class);
    }

    public Boolean selectBoolean() {
        return selectOne(Boolean.class);
    }

    private <T> T selectOne(Class<T> cls) {
        return simpleDb.execute(sql, params.toArray(), stmt -> {
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;

                Object value = rs.getObject(1);

                if (cls == Boolean.class && value instanceof Number number) {
                    return cls.cast(number.intValue() == 1);
                }

                return cls.cast(value);
            }
        });
    }

    private <T> List<T> selectOneList(Class<T> cls) {
        return simpleDb.execute(sql, params.toArray(), stmt -> {
            try (ResultSet rs = stmt.executeQuery()) {
                List<T> list = new ArrayList<>();

                while (rs.next()) {
                    list.add(cls.cast(rs.getObject(1)));
                }

                return list;
            }
        });
    }
}
