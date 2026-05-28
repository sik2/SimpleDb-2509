package com.back.simpleDb;

import com.back.Article;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class Sql {
    private final Connection connection;
    private final StringBuilder query;
    private final List<Object> params;

    public Sql(Connection connection) {
        this.connection = connection;
        this.query = new StringBuilder();
        this.params = new ArrayList<>();
    }

    public Sql append(String sql, Object... args) {
        query.append(" ").append(sql);
        Collections.addAll(params, args);
        return this;
    }

    public Sql appendIn(String sql, Object... args) {
        String expanded = sql.replace("?", String.join(",", Collections.nCopies(args.length, "?")));
        return append(expanded, args);
    }

    private void setParam(PreparedStatement ps) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    public long insert() {
        try (PreparedStatement ps = connection.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS)) {
            setParam(ps);
            ps.execute();
            var rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("삽입되지 않았습니다.");
    }

    public int update() {
        try (PreparedStatement ps = connection.prepareStatement(query.toString())) {
            setParam(ps);
            return ps.executeUpdate();
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int delete() {
        return update();
    }

    public List<Map<String, Object>> selectRows() {
        try (PreparedStatement ps = connection.prepareStatement(query.toString())) {
            setParam(ps);
            var rs = ps.executeQuery();
            var meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new java.util.HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
            return rows;
        } catch(SQLException e) {
            throw new RuntimeException(e);

        }
    }

    public Map<String, Object> selectRow() {
        try (PreparedStatement ps = connection.prepareStatement(query.toString())){
            setParam(ps);
            var rs = ps.executeQuery();
            var meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            Map<String, Object> row = new java.util.HashMap<>();
            if (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
            }
            return row;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public LocalDateTime selectDatetime() {
        try (PreparedStatement ps = connection.prepareStatement(query.toString())) {
            setParam(ps);
            var rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getTimestamp(1).toLocalDateTime();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("날짜가 조회되지 않았습니다.");
    }

    public Long selectLong() {
        try (PreparedStatement ps  = connection.prepareStatement(query.toString())) {
            setParam(ps);
            var rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("long 값이 조회되지 않았습니다.");
    }

    public String selectString() {
        try (PreparedStatement ps  = connection.prepareStatement(query.toString())) {
            setParam(ps);
            var rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("string 값이 조회되지 않았습니다.");
    }

    public Boolean selectBoolean() {
        try (PreparedStatement ps = connection.prepareStatement(query.toString())) {
            setParam(ps);
            var rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("boolean 값이 조회되지 않았습니다.");
    }

    public List<Long> selectLongs() {
        try (PreparedStatement ps = connection.prepareStatement(query.toString())) {
            setParam(ps);
            var rs = ps.executeQuery();
            List<Long> result = new ArrayList<>();
            while (rs.next()) {
                result.add(rs.getLong(1));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> List<T> selectRows(Class<T> cls) {
        try (PreparedStatement ps = connection.prepareStatement(query.toString())) {
            setParam(ps);
            var rs = ps.executeQuery();
            List<T> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapRow(rs, cls));
            }
            return rows;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T selectRow(Class<T> cls) {
        try (PreparedStatement ps = connection.prepareStatement(query.toString())) {
            setParam(ps);
            var rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs, cls);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Article 객체가 조회되지 않았습니다.");
    }

    private <T> T mapRow(ResultSet rs, Class<T> cls) throws Exception {
        T obj = cls.getDeclaredConstructor().newInstance();
        var meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String colName = meta.getColumnLabel(i);
            Object colValue = rs.getObject(i);
            String setterName = (colName.startsWith("is"))?
                    "set" + colName.substring(2, 3).toUpperCase() + colName.substring(3) :
                    "set" + colName.substring(0, 1).toUpperCase() + colName.substring(1);
            var setter = Arrays.stream(cls.getMethods())
                    .filter(m -> m.getName().equals(setterName) && m.getParameterCount() == 1)
                    .findFirst()
                    .orElse(null);
            if (setter != null) {
                Class<?> paramType = setter.getParameterTypes()[0];
                if (colValue instanceof Timestamp && paramType == LocalDateTime.class) {
                    colValue = ((Timestamp) colValue).toLocalDateTime();
                }
                setter.invoke(obj, colValue);
            }
        }
        return obj;
    }
}
