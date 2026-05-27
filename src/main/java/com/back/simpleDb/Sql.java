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

    public int update() { throw new UnsupportedOperationException("Not implemented yet"); }
    public int delete() { throw new UnsupportedOperationException("Not implemented yet"); }
    public List<Map<String, Object>> selectRows() { throw new UnsupportedOperationException("Not implemented yet"); }
    public Map<String, Object> selectRow() { throw new UnsupportedOperationException("Not implemented yet"); }
    public Long selectLong() { throw new UnsupportedOperationException("Not implemented yet"); }
    public List<Long> selectLongs() { throw new UnsupportedOperationException("Not implemented yet"); }
    public String selectString() { throw new UnsupportedOperationException("Not implemented yet"); }
    public Boolean selectBoolean() { throw new UnsupportedOperationException("Not implemented yet"); }
    public LocalDateTime selectDatetime() { throw new UnsupportedOperationException("Not implemented yet"); }
    public Sql appendIn(String sql, Object... values) { throw new UnsupportedOperationException("Not implemented yet"); }
    public <T> List<T> selectRows(Class<T> clazz) { throw new UnsupportedOperationException("Not implemented yet"); }
    public <T> T selectRow(Class<T> clazz) { throw new UnsupportedOperationException("Not implemented yet"); }
}
