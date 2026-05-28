package com.back.simpleDb;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

@RequiredArgsConstructor
public class Sql {
    private final SimpleDb simpleDb;
    private final StringBuilder sqlBuilder = new StringBuilder();
    private final List<Object> params = new ArrayList<>();


    public Sql append(String sql) {
        sqlBuilder.append(" ").append(sql);
        return this;
    }

    public Sql append(String sql, Object... args) {
        sqlBuilder.append(" ").append(sql);
        for (Object arg : args) {
            params.add(arg);
        }
        return this;
    }

    public Sql appendIn(String sql, Object... args) {

        //Collections.nCopies(int n, T obj) → obj를 n번 반복한 불변 리스트 반환
        // args 개수만큼 placeholder(?) 생성
        String placeholder = String.join(",", Collections.nCopies(args.length, "?"));

        String changedSql = sql.replace("?", placeholder);

        sqlBuilder.append(" ").append(changedSql);

        for (Object arg : args) {
            params.add(arg);
        }

        return this;

    }


    //params 리스트에 있는 값들을 SQL의 플레이스홀더(?)에 순서대로 바인딩 -> 바인딩 완료된 PreparedStatement 반환
    private PreparedStatement buildPreparedStatement(PreparedStatement ps) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
        return ps;
    }


    private void printLog(String sql) {
        if (simpleDb.isDevMode()) {
            System.out.println("== rawSql ==");
            System.out.println(sql);
            System.out.println("params: " + params);
        }
    }

    // 생성된 Auto Increment PK 반환
    public long insert() {
        String sql = sqlBuilder.toString().trim();

        printLog(sql);

        try (PreparedStatement ps = buildPreparedStatement(simpleDb.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))) {

            ps.executeUpdate(); //INSERT/UPDATE/DELETE 전용, 영향받은 행 수 반환

            // AUTO_INCREMENT PK 꺼내기
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return 0;
    }

    private int executeUpdate() {
        String sql = sqlBuilder.toString().trim();
        printLog(sql);

        try (PreparedStatement ps = buildPreparedStatement(
                simpleDb.getConnection().prepareStatement(sql))) {

            return ps.executeUpdate();   //INSERT/UPDATE/DELETE 전용, 영향받은 행 수 반환

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

    // 타입 변환 : DATETIME → LocalDateTime, BIT(1) → Boolean
    private Object convertValue(Object value) {
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (value instanceof byte[] bytes) {
            return bytes[0] != 0;
        }
        return value;
    }

    //ResultSet 한행 -> Map 변환
    private Map<String, Object> mapRow(ResultSet rs, ResultSetMetaData metaData) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            Object value = convertValue(rs.getObject(i));
            row.put(columnName, value);
        }
        return row;
    }

    //ResultSet 전체 순회 -> List<Map> 반환
    private List<Map<String, Object>> mapRows(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        List<Map<String, Object>> rows = new ArrayList<>();

        while (rs.next()) {
            rows.add(mapRow(rs, metaData));
        }
        return rows;
    }

    public List<Map<String, Object>> selectRows() {
        String sql = sqlBuilder.toString().trim();
        printLog(sql);

        try (PreparedStatement ps = buildPreparedStatement(
                simpleDb.getConnection().prepareStatement(sql));
             ResultSet rs = ps.executeQuery()) {    //SELECT 전용, ResultSet (조회 결과) 반환

            return mapRows(rs);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> List<T> selectRows(Class<T> clazz) {
        List<Map<String, Object>> rows = selectRows();
        return rows.stream()
                .map(row -> mapToObj(row, clazz))
                .toList();
    }

    //
    private <T> T mapToObj(Map<String, Object> row, Class<T> clazz) {
        try {
            //Article 객체 생성
            T obj = clazz.getDeclaredConstructor().newInstance();
            //= new Article()

            //Map의 키-값을 field에 주입
            row.forEach((key, val) -> {

                try {
                    Field field = clazz.getDeclaredField(key);
                    field.setAccessible(true);  // private 접근 허용
                    field.set(obj, val);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            });
            return obj;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    public <T> T selectRow(Class<T> clazz) {
        List<T> rows = selectRows(clazz);

        if (rows.isEmpty()) return null;

        return rows.get(0); // 첫번째 행만 반환
    }


    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();
        return rows.isEmpty() ? null : rows.get(0);
    }

//    public LocalDateTime selectDatetime() {
//        String sql = sqlBuilder.toString().trim();
//        printLog(sql);
//
//        try (PreparedStatement ps = buildPreparedStatement(
//                simpleDb.getConnection().prepareStatement(sql));
//
//             ResultSet rs = ps.executeQuery()) {
//            if (rs.next()) {
//                return rs.getTimestamp(1).toLocalDateTime();
//            }
//
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//        return null;
//    }
//
//    public Long selectLong() {
//        String sql = sqlBuilder.toString().trim();
//        printLog(sql);
//
//        try (PreparedStatement ps = buildPreparedStatement(
//                simpleDb.getConnection().prepareStatement(sql));
//             ResultSet rs = ps.executeQuery();
//        ) {
//            if (rs.next()) {
//                return rs.getLong(1);
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//        return null;
//    }


    private <T> T selectSingle(SqlResultMapper<T> mapper) {
        String sql = sqlBuilder.toString().trim();
        printLog(sql);

        try (PreparedStatement ps = buildPreparedStatement(
                simpleDb.getConnection().prepareStatement(sql));
             ResultSet rs = ps.executeQuery();
        ) {
            if (rs.next()) {
                return mapper.map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;

    }

    private <T> List<T> selectList(SqlResultMapper<T> mapper) {
        String sql = sqlBuilder.toString().trim();
        printLog(sql);

        try (PreparedStatement ps = buildPreparedStatement(
                simpleDb.getConnection().prepareStatement(sql));
             ResultSet rs = ps.executeQuery()) {

            List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapper.map(rs));
            }
            return results;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    interface SqlResultMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    public Long selectLong() {
        return selectSingle(rs -> rs.getLong(1));
    }

    public String selectString() {
        return selectSingle(rs -> rs.getString(1));
    }

    public Boolean selectBoolean() {
        return selectSingle(rs -> rs.getBoolean(1));
    }

    public LocalDateTime selectDatetime() {
        return selectSingle(rs -> rs.getTimestamp(1).toLocalDateTime());
    }


    public List<Long> selectLongs() {
        return selectList(rs -> rs.getLong(1));
    }
}
