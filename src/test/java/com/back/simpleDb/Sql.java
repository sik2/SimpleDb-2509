package com.back.simpleDb;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class Sql {

    private Connection connection;
    private StringBuilder sb = new StringBuilder();

    public Sql(Connection connection) {
        this.connection = connection;
    }

    public Sql append(String str) {
        sb.append(str);
        sb.append(" ");
        return this;
    }

    public Sql append(String query, Object... params) {
        String result = query;

        for (Object param : params) {
            String value;

            if (param instanceof String) {
                value = "'" + param + "'";
            } else {
                value = param.toString();
            }

            result = result.replaceFirst("\\?", value);
        }

        sb.append(result);
        sb.append(" ");
        return this;
    }

    public long insert() {
        String query = sb.toString();
        System.out.println(query);

        try (
                PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
        ) {
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();

            if (rs.next()) {
                long id = rs.getLong(1);
                return id;
        }
            }catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public int update() {
        String query = sb.toString();

        try (
                PreparedStatement stmt = connection.prepareStatement(query)
        ) {
            int affectedRows = stmt.executeUpdate();
            return affectedRows;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public int delete() {
        String query = sb.toString();

        try (
                PreparedStatement stmt = connection.prepareStatement(query)
        ) {
            int affectedRows = stmt.executeUpdate();
            return affectedRows;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    List<Map<String, Object>> selectRows() {
        String query = sb.toString();
        List<Map<String, Object>> rows = new ArrayList<>();

        try (
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet resultSet = stmt.executeQuery();
        ) {
            ResultSetMetaData metaData = resultSet.getMetaData();

            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }

                rows.add(row);
            }
        } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        return rows;
    }

    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = this.selectRows();
        return rows.get(0);
    }

    public LocalDateTime selectDatetime() {
        String query = sb.toString();

        try(
                Statement stmt = connection.createStatement();
                ResultSet resultSet = stmt.executeQuery(query);
                ) {
            if (resultSet.next()) {
                LocalDateTime dateTime = resultSet.getObject(1, LocalDateTime.class);
                return dateTime;
            }
        } catch(SQLException e){
            e.printStackTrace();
        }

        return null;
    }

    public Long selectLong() {
        Map<String, Object> row = this.selectRow();

        for (String key : row.keySet()) {
            Object value = row.get(key);

            if (value instanceof Long) {
                return (Long) value;
            }
        }

        return null;
    }

    public String selectString() {
        Map<String, Object> row = this.selectRow();

        for (String key : row.keySet()) {
            Object value = row.get(key);

            if (value instanceof String) {
                return (String) value;
            }
        }

        return null;
    }

    public Boolean selectBoolean() {
        Map<String, Object> row = this.selectRow();

        for (String key : row.keySet()) {
            Object value = row.get(key);

            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            else if (value instanceof Number number) {
                return number.intValue() != 0;
            }
        }

        return null;
    }

    public Sql appendIn(String str, Object... params) {
        StringJoiner joiner = new StringJoiner(", ");

        // 배열 하나만 들어온 경우
        if (params.length == 1 && params[0] instanceof Object[] array) {
            for (Object value : array) {
                if (value instanceof String s) {
                    value = "'" + s + "'";
                }
                joiner.add(value.toString());
            }
        }
        else {
            for (Object value : params) {
                if (value instanceof String s) {
                    value = "'" + s + "'";
                }
                joiner.add(value.toString());
            }
        }

        String parsedQuery = str.replace("?", joiner.toString());
        sb.append(" ").append(parsedQuery);
        return this;
    }

    public List<Long> selectLongs() {
        String query = sb.toString();
        List<Long> longs = new ArrayList<>();

        try (
                PreparedStatement stmt = connection.prepareStatement(query);
                ResultSet resultSet = stmt.executeQuery();
        ) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            while (resultSet.next()) {
                Object value = resultSet.getObject(1);
                longs.add((Long) value);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return longs;
    }

    public <T> List<T> selectRows(Class<T> cls) {
        String query = sb.toString();
        List<T> list = new ArrayList<>();
        Constructor<T> constructor = null;
        try {
            constructor = cls.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        try (
                PreparedStatement stmt = connection.prepareStatement(query);
                ResultSet resultSet = stmt.executeQuery();
        ) {
            ResultSetMetaData metaData = resultSet.getMetaData();

            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                // 객체 생성
                T instance = constructor.newInstance();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);

                    try {
                        Field field = cls.getDeclaredField(columnName);
                        field.setAccessible(true);

                        Object value = resultSet.getObject(i);

                        field.set(instance, value);
                    } catch (NoSuchFieldException e) {
                        // 필드 없으면 무시
                    }
                }

                list.add(instance);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public <T> T selectRow(Class<T> cls) {
        List<T> list = this.selectRows(cls);
        if (list.isEmpty()) {
            return null;
        }

        return list.get(0);
    }
}
