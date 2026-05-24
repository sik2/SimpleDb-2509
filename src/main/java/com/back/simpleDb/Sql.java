package com.back.simpleDb;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Sql {
    private final SimpleDb simpleDb;
    private final StringBuilder sqlBuilder = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sqlPart, Object... args) {
        if (!sqlBuilder.isEmpty()) {
            sqlBuilder.append("\n");
        }
        sqlBuilder.append(sqlPart);
        for (Object arg : args) {
            params.add(arg);
        }
        return this;
    }

    public long insert() {
        String sql = sqlBuilder.toString();

        try (var conn = simpleDb.getConnection();
             var pstmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            SimpleDb.bindParams(pstmt, params.toArray());
            pstmt.executeUpdate();

            try (var rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

            return 0L;
        } catch (Exception e) {
            throw new RuntimeException("INSERT 실행 중 오류가 발생했습니다.", e);
        }
    }

    public Sql appendIn(String sqlPart, Object... args) { throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다."); }
    public int update() { throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다."); }
    public int delete() { throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다."); }
    public List<Map<String, Object>> selectRows() { throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다."); }
    public Map<String, Object> selectRow() { throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다."); }
    public LocalDateTime selectDatetime() { throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다."); }
    public Long selectLong() { throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다."); }
    public String selectString() { throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다."); }
    public Boolean selectBoolean() { throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다."); }
    public List<Long> selectLongs() { throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다."); }
    public <T> List<T> selectRows(Class<T> clazz) { throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다."); }
    public <T> T selectRow(Class<T> clazz) { throw new UnsupportedOperationException("아직 구현되지 않은 기능입니다."); }
}
