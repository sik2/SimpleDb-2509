package com.back.simpleDb;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class Sql {

    private final SimpleDb simpleDb;
    private final StringBuilder sqlBuilder = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sql, Object... bindParams) {
        if (sqlBuilder.length() > 0) sqlBuilder.append("\n");
        sqlBuilder.append(sql.trim());
        Collections.addAll(params, bindParams);
        return this;
    }

    private String buildSql() {
        return sqlBuilder.toString();
    }

    private void bindParams(PreparedStatement pstmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            pstmt.setObject(i + 1, params.get(i));
        }
    }

    public long insert() {
        String sql = buildSql();
        Connection conn = simpleDb.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindParams(pstmt, params);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("INSERT failed: " + e.getMessage(), e);
        }
        return 0L;
    }

    public int update() { throw new UnsupportedOperationException(); }
    public int delete() { throw new UnsupportedOperationException(); }
    public List<Map<String, Object>> selectRows() { throw new UnsupportedOperationException(); }
    public Map<String, Object> selectRow() { throw new UnsupportedOperationException(); }
    public LocalDateTime selectDatetime() { throw new UnsupportedOperationException(); }
    public Long selectLong() { throw new UnsupportedOperationException(); }
    public List<Long> selectLongs() { throw new UnsupportedOperationException(); }
    public String selectString() { throw new UnsupportedOperationException(); }
    public Boolean selectBoolean() { throw new UnsupportedOperationException(); }
    public Sql appendIn(String sql, Object... values) { throw new UnsupportedOperationException(); }
    public <T> T selectRow(Class<T> clazz) { throw new UnsupportedOperationException(); }
    public <T> List<T> selectRows(Class<T> clazz) { throw new UnsupportedOperationException(); }
}