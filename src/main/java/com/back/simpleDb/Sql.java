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

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sql, Object... params) {
        if (!sqlBuilder.isEmpty()) sqlBuilder.append(" ");
        sqlBuilder.append(sql);
        this.params.addAll(Arrays.asList(params));
        return this;
    }

    public Sql appendIn(String sql, Object... params) {
        String placeholders = String.join(", ", Collections.nCopies(params.length, "?"));
        String expandedSql = sql.replaceFirst("\\?", placeholders);
        if (!sqlBuilder.isEmpty()) sqlBuilder.append(" ");
        sqlBuilder.append(expandedSql);
        this.params.addAll(Arrays.asList(params));
        return this;
    }

    public long insert() {
        try (PreparedStatement pstmt = buildStatement(Statement.RETURN_GENERATED_KEYS)) {
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
                throw new RuntimeException("삽입 실패");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int update() {
        try (PreparedStatement pstmt = buildStatement()) {
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int delete() {
        try (PreparedStatement pstmt = buildStatement()) {
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, Object>> selectRows() {
        try (PreparedStatement pstmt = buildStatement();
             ResultSet rs = pstmt.executeQuery()) {
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapRow(rs));
            }
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();
        return rows.isEmpty() ? null : rows.get(0);
    }

    public <T> List<T> selectRows(Class<T> clazz) {
        return selectRows().stream()
                .map(row -> objectMapper.convertValue(row, clazz))
                .toList();
    }

    public <T> T selectRow(Class<T> clazz) {
        Map<String, Object> row = selectRow();
        return row == null ? null : objectMapper.convertValue(row, clazz);
    }

    public LocalDateTime selectDatetime() {
        try (PreparedStatement pstmt = buildStatement();
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp(1);
                return ts != null ? ts.toLocalDateTime() : null;
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Long selectLong() {
        try (PreparedStatement pstmt = buildStatement();
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                long val = rs.getLong(1);
                return rs.wasNull() ? null : val;
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String selectString() {
        try (PreparedStatement pstmt = buildStatement();
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getString(1);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Boolean selectBoolean() {
        try (PreparedStatement pstmt = buildStatement();
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                boolean val = rs.getBoolean(1);
                return rs.wasNull() ? null : val;
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Long> selectLongs() {
        try (PreparedStatement pstmt = buildStatement();
             ResultSet rs = pstmt.executeQuery()) {
            List<Long> result = new ArrayList<>();
            while (rs.next()) {
                result.add(rs.getLong(1));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    void execute() {
        try (PreparedStatement pstmt = buildStatement()) {
            pstmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String colName = meta.getColumnLabel(i);
            int sqlType = meta.getColumnType(i);
            Object value;
            if (sqlType == Types.BIT || sqlType == Types.BOOLEAN) {
                boolean b = rs.getBoolean(i);
                value = rs.wasNull() ? null : b;
            } else if (sqlType == Types.TIMESTAMP) {
                Timestamp ts = rs.getTimestamp(i);
                value = ts != null ? ts.toLocalDateTime() : null;
            } else {
                value = rs.getObject(i);
            }
            row.put(colName, value);
        }
        return row;
    }

    private PreparedStatement buildStatement() throws SQLException {
        return buildStatement(Statement.NO_GENERATED_KEYS);
    }

    private PreparedStatement buildStatement(int autoGeneratedKeys) throws SQLException {
        String sql = sqlBuilder.toString();
        if (simpleDb.isDevMode()) {
            System.out.println("== rawSql ==\n" + sql);
        }
        PreparedStatement pstmt = simpleDb.getConnection().prepareStatement(sql, autoGeneratedKeys);
        for (int i = 0; i < params.size(); i++) {
            pstmt.setObject(i + 1, params.get(i));
        }
        return pstmt;
    }
}