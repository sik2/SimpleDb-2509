package com.back.simpleDb;

import lombok.SneakyThrows;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class Sql {
    private final SimpleDb simpleDb;

    // sql 쿼리를 담는 StringBuilder와 쿼리의 파라미터를 담는 List
    private final StringBuilder stringBuilder = new StringBuilder();
    private final List<Object> param = new ArrayList<>();

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    // 개별 인자로 전달하기 위한 오버로딩된 append 메서드
    public Sql append(String sql, Object... params) {
        stringBuilder.append(sql).append(" ");
        this.param.addAll(Arrays.asList(params));

        return this;
    }

    // 개별 인자로 전달하기 위한 오버로딩된 appendIn 메서드
    public Sql appendIn(String sql, Object... params) {
        return this;
    }

    // 개별 인자로 전달받은 sql과 params를 사용하여 PreparedStatement를 생성하는 메서드
    @SneakyThrows
    private PreparedStatement buildStatement(boolean returnKeys) {
        Connection connection = simpleDb.getConnection();

        PreparedStatement ps = connection.prepareStatement(
                stringBuilder.toString(),
                Statement.RETURN_GENERATED_KEYS
        );

        for (int i=0; i<param.size(); i++) {
            ps.setObject(i + 1, param.get(i));
        }

        return ps;
    }

    @SneakyThrows
    public long insert() {
        // 생성한 id가 필요하므로 true 입력
        PreparedStatement ps = buildStatement(true);
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        rs.next();
        return rs.getLong(1);
    }

    @SneakyThrows
    public int update() {
        return buildStatement(false).executeUpdate();
    }

    @SneakyThrows
    public int delete() {
        return buildStatement(false).executeUpdate();
    }

    @SneakyThrows
    public List<Map<String, Object>> selectRows() {
        List<Map<String, Object>> result = new ArrayList<>();

        PreparedStatement ps = buildStatement(false);
        ResultSet rs = ps.executeQuery();
        ResultSetMetaData metaData = rs.getMetaData();

        // 쿼리 끝까지 반복
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            // 리스트에 각 컬럼값 입력
            for (int i=1; i<=metaData.getColumnCount(); i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(i);
                row.put(columnName, value);
            }
            result.add(row);
        }
        return result;
    }

    @SneakyThrows
    public <T> List<T> selectRows(Class<T> cls) {
        List<T> result = new ArrayList<>();

        PreparedStatement ps = buildStatement(false);
        ResultSet rs = ps.executeQuery();
        ResultSetMetaData metaData = rs.getMetaData();

        // 쿼리 끝까지 반복
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            // 리스트에 각 컬럼값 입력
            for (int i=1; i<=metaData.getColumnCount(); i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(i);
                row.put(columnName, value);
            }
        }
        return result;
    }

    @SneakyThrows
    public Map<String, Object> selectRow() {
        // selectRows() 메서드를 호출하여 결과를 가져오고, 결과가 비어있지 않으면 첫 번째 행을 반환
        List<Map<String, Object>> rows = selectRows();

        return rows.isEmpty() ? null : rows.get(0);
    }

    public <T> T selectRow(Class<T> cls) {
        return null;
    }

    @SneakyThrows
    public LocalDateTime selectDatetime() {
        PreparedStatement ps = buildStatement(false);
        ResultSet rs = ps.executeQuery();
        rs.next();

        // SELECT NOW()를 실행하면 결과는 1개(시간)이므로
        // rs.getTimestamp(1)로 첫 번째 컬럼의 값을 가져와
        // LocalDateTime으로 변환하여 반환
        // getTimeStamp는 시간대 차이로 인해 getObject로 변경
        return rs.getObject(1, LocalDateTime.class);
    }

    @SneakyThrows
    public Long selectLong() {
        PreparedStatement ps = buildStatement(false);
        ResultSet rs = ps.executeQuery();
        rs.next();

        // Sql 실행 결과는 id이므로, selectDateTime과 구조적으로 동일함
        return rs.getObject(1, Long.class);
    }

    public List<Long> selectLongs() {
        return null;
    }

    @SneakyThrows
    public String selectString() {
        PreparedStatement ps = buildStatement(false);
        ResultSet rs = ps.executeQuery();
        rs.next();

        return rs.getObject(1, String.class);
    }

    @SneakyThrows
    public Boolean selectBoolean() {
        PreparedStatement ps = buildStatement(false);
        ResultSet rs = ps.executeQuery();
        rs.next();

        return rs.getObject(1, Boolean.class);
    }
}
