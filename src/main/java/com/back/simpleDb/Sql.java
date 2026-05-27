package com.back.simpleDb;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class Sql {
    private final SimpleDb simpleDb;
    private final StringBuilder sqlBuilder = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sql, Object... appendParams) {
        if (sqlBuilder.length() > 0) sqlBuilder.append("\n");
        sqlBuilder.append(sql);
        Collections.addAll(params, appendParams);
        return this;
    }

    private PreparedStatement prepare(Connection conn) throws SQLException {
        String sql = sqlBuilder.toString();
        if (simpleDb.isDevMode()) {
            System.out.println("== rawSql ==");
            System.out.println(sql);
            System.out.println("params: " + params);
        }
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
        return ps;
    }

    void execute() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement ps = prepare(conn)) {
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException("SQL 실행 실패: " + e.getMessage(), e);
        }
    }

    public long insert() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement ps = prepare(conn)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
            return 0L;
        } catch (SQLException e) {
            throw new RuntimeException("INSERT 실패: " + e.getMessage(), e);
        }
    }

    public int update() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement ps = prepare(conn)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("UPDATE 실패: " + e.getMessage(), e);
        }
    }
    public int delete() { return update(); }
    private Object convertValue(Object value) {
        if (value instanceof Timestamp) return ((Timestamp) value).toLocalDateTime();
        if (value instanceof byte[]) { byte[] b = (byte[]) value; return b.length > 0 && b[0] != 0; }
        return value;
    }

    public List<Map<String, Object>> selectRows() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement ps = prepare(conn); ResultSet rs = ps.executeQuery()) {
            List<Map<String, Object>> rows = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++)
                    row.put(meta.getColumnLabel(i), convertValue(rs.getObject(i)));
                rows.add(row);
            }
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("SELECT 실패: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();
        return rows.isEmpty() ? null : rows.get(0);
    }
    public LocalDateTime selectDatetime() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement ps = prepare(conn); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) { Timestamp ts = rs.getTimestamp(1); return ts != null ? ts.toLocalDateTime() : null; }
            return null;
        } catch (SQLException e) { throw new RuntimeException("selectDatetime 실패", e); }
    }

    public Long selectLong() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement ps = prepare(conn); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
            return null;
        } catch (SQLException e) { throw new RuntimeException("selectLong 실패", e); }
    }
    public List<Long> selectLongs() { throw new UnsupportedOperationException("Not implemented yet"); }
    public String selectString() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement ps = prepare(conn); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString(1);
            return null;
        } catch (SQLException e) { throw new RuntimeException("selectString 실패", e); }
    }
    public Boolean selectBoolean() {
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement ps = prepare(conn); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getBoolean(1);
            return null;
        } catch (SQLException e) { throw new RuntimeException("selectBoolean 실패", e); }
    }
    public Sql appendIn(String sql, Object... values) { throw new UnsupportedOperationException("Not implemented yet"); }
    public <T> List<T> selectRows(Class<T> clazz) { throw new UnsupportedOperationException("Not implemented yet"); }
    public <T> T selectRow(Class<T> clazz) { throw new UnsupportedOperationException("Not implemented yet"); }
}
