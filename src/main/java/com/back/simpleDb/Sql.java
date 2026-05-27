package com.back.simpleDb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    public int update() {
        try (PreparedStatement ps = simpleDb.getConnection().prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * java 로직은 sql 문자열과 파라미터를 DB에 전달하고 실행 결과를 받는 역할
     * update인지 delete인지 문법을 해석하고 실제로 테이블을 바꾸는 일은 MySql이 한다
     */

    public int delete() {
        try (PreparedStatement ps = simpleDb.getConnection().prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, Object>> selectRows() {
        return null;
    }

    public <T> List<T> selectRows(Class<T> clazz) {
        return null;
    }

    public Map<String, Object> selectRow() {
        return null;
    }

    public <T> T selectRow(Class<T> clazz) {
        return null;
    }

    public LocalDateTime selectDatetime() {
        return null;
    }

    public Long selectLong() {
        return null;
    }

    public String selectString() {
        return null;
    }

    public Boolean selectBoolean() {
        return null;
    }

    public List<Long> selectLongs() {
        return null;
    }
}