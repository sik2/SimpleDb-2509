package com.back.simpleDb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Sql {
    private final Connection conn;
    private final boolean devMode;
    private final StringBuilder query = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    public Sql(Connection conn, boolean devMode) {
        this.conn = conn;
        this.devMode = devMode;
    }

    public Sql append(String sql, Object... args) {
        if (!query.isEmpty()) query.append(" ");
        query.append(sql);
        Collections.addAll(params, args);
        return this;
    }

    private PreparedStatement buildPs() throws SQLException {
        String rawSql = query.toString();
        if (devMode) {
            System.out.println("== rawSql ==");
            System.out.println(rawSql);
            System.out.println("params: " + params);
        }
        PreparedStatement ps = conn.prepareStatement(
                rawSql, Statement.RETURN_GENERATED_KEYS
        );
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
        return ps;
    }

    public void run() {
        try (PreparedStatement ps = buildPs()) {
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public long insert() {
        try (PreparedStatement ps = buildPs()) {
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int update() {
        try (PreparedStatement ps = buildPs()) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int delete() {
        return update();
    }



    private Map<String, Object> resultSetToMap(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        Map<String, Object> map = new LinkedHashMap<>();

        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String col = meta.getColumnName(i);
            Object val = rs.getObject(i);
            map.put(col, convertValue(val));
        }
        return map;
    }

    private Object convertValue(Object val) {
        if (val == null) return null;

        if (val instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }

        if (val instanceof byte[] bytes) {
            return bytes[0] != 0;
        }

        return val;
    }

    public Map<String, Object> selectRow() {
        try (PreparedStatement ps = buildPs();
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return null;
            return resultSetToMap(rs);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, Object>> selectRows() {
        try (PreparedStatement ps = buildPs();
             ResultSet rs = ps.executeQuery()) {
            List<Map<String, Object>> result = new ArrayList<>();
            while (rs.next()) {
                result.add(resultSetToMap(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public LocalDateTime selectDatetime() {
        try (PreparedStatement ps = buildPs();
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            Timestamp ts = rs.getTimestamp(1);
            return ts != null ? ts.toLocalDateTime() : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Long selectLong() {
        try (PreparedStatement ps = buildPs();
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String selectString() {
        try (PreparedStatement ps = buildPs();
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getString(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public Boolean selectBoolean() {
        try (PreparedStatement ps = buildPs();
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            Object val = rs.getObject(1);

            if (val instanceof byte[] bytes) return bytes[0] != 0;
            if (val instanceof Boolean b) return b;
            if (val instanceof Number n) return n.intValue() != 0;

            return false;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Sql appendIn(String sql, Object... args) {
        String placeholders = IntStream.range(0, args.length)
                .mapToObj(i -> "?")
                .collect(Collectors.joining(", "));

        int idx = sql.indexOf('?');
        String expandedSql = sql.substring(0, idx) + placeholders + sql.substring(idx + 1);

        if (!query.isEmpty()) query.append(" ");
        query.append(expandedSql);
        Collections.addAll(params, args);
        return this;
    }

    public List<Long> selectLongs() {
        try (PreparedStatement ps = buildPs();
             ResultSet rs = ps.executeQuery()) {
            List<Long> result = new ArrayList<>();
            while (rs.next()) {
                result.add(rs.getLong(1));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public <T> T selectRow(Class<T> clazz) {
        Map<String, Object> row = selectRow();
        if (row == null) return null;
        return mapper.convertValue(row, clazz);
    }

    public <T> List<T> selectRows(Class<T> clazz) {
        return selectRows().stream()
                .map(row -> mapper.convertValue(row, clazz))
                .collect(Collectors.toList());
    }

}