package com.back.simpleDb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Sql {
    private final SimpleDb simpleDb;
    //append가 여러번 호출되니 이어붙여 보관
    private final StringBuilder sqlBuilder = new StringBuilder();
    //누적해놓고 바인딩할 값 순서대로 보관
    private final List<Object> params = new ArrayList<>();

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sql, Object... params) {
        sqlBuilder.append(sql).append(" ");
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            this.params.add(param);
        }
        return this;
    }

    public Sql appendIn(String sql, Object... params) {
        return this;
    }

    // i + 1은 java의 인덱스는 0번부 시작
    public long insert() {
        try (PreparedStatement ps = simpleDb.getConnection()
                .prepareStatement(sqlBuilder.toString(), Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * java 로직은 sql 문자열과 파라미터를 DB에 전달하고 실행 결과를 받는 역할
     * update인지 delete인지 문법을 해석하고 실제로 테이블을 바꾸는 일은 MySql이 한다
     */

    private int executeUpdate() {
        try (PreparedStatement ps = simpleDb.getConnection().prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int update() {
        return executeUpdate();
    }

    public int delete() {
        return executeUpdate();
    }

    public List<Map<String, Object>> selectRows() {
        try (PreparedStatement ps = simpleDb.getConnection().prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            //select 조회문 사용
            ResultSet rs = ps.executeQuery();
            //조회 컬럼 정보 가져옴
            ResultSetMetaData metaData = rs.getMetaData();

            //조회 결과의 컬럼 개수 가져옴
            int columnCount = metaData.getColumnCount();
            //최종 결과 담을 리스트 만듬
            List<Map<String, Object>> rows = new ArrayList<>();

            while (rs.next()) {
                //LinkedHashMap은 입력한 순서대로 key, value를 보관하는 Map
                Map<String, Object> row = new LinkedHashMap<>();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    //컬럼마다 타입 다름
                    Object value = rs.getObject(i);

                    row.put(columnName, value);
                }
                rows.add(row);
            }
            return rows;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> List<T> selectRows(Class<T> clazz) {
        return null;
    }

    /*
    * selectRows()는 MySQL이 찾아서 반환한 ResultSet을 컬럼명과 컬럼값으로 묶어서
    * List<Map<String, Object>>로 바꿔준다.
    * selectRow()는 그 결과 리스트에서 첫 번째 row만 꺼내서 반환한다.
    */
    public Map<String, Object> selectRow() {
        List<Map<String, Object>> rows = selectRows();

        if(rows.isEmpty()) {
            return null;
        }
        return rows.get(0);
    }

    public <T> T selectRow(Class<T> clazz) {
        return null;
    }

    public LocalDateTime selectDatetime() {
        Map<String, Object> row = selectRow();

        if(row == null) {
            return null;
        }

        Object value = row.values().iterator().next();

        return (LocalDateTime) value;
    }

    public Long selectLong() {
        Map<String, Object> row = selectRow();

        if(row == null) {
            return null;
        }

        Object value = row.values().iterator().next();

        return (Long) value;
    }

    public String selectString() {
        Map<String, Object> row = selectRow();

        if(row == null) {
            return null;
        }

        Object value = row.values().iterator().next();

        return (String) value;
    }

    public Boolean selectBoolean() {
        Map<String, Object> row = selectRow();

        if(row == null) {
            return null;
        }

        Object value = row.values().iterator().next();

        if(value instanceof Boolean) {
            return (Boolean) value;
        }

        return ((Number)value).intValue() == 1;
    }

    public List<Long> selectLongs() {
        return null;
    }
}