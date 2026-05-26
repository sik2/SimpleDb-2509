package com.back.simpleDb;

import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    @SneakyThrows
    public long insert() {
        Connection connection = simpleDb.getConnection();

        PreparedStatement ps = connection.prepareStatement(
                stringBuilder.toString(),
                Statement.RETURN_GENERATED_KEYS
        );

        for (int i=0; i<param.size(); i++) {
            ps.setObject(i + 1, param.get(i));
        }

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        rs.next();
        return rs.getLong(1);
    }

    @SneakyThrows
    public int update() {
        Connection connection = simpleDb.getConnection();
        PreparedStatement ps = connection.prepareStatement(
                stringBuilder.toString()
        );

        for (int i=0; i<param.size(); i++) {
            ps.setObject(i + 1, param.get(i));
        }

        ps.executeUpdate();
        return ps.getUpdateCount();
    }

    public int delete() {
        return 0;
    }

    public List<Map<String, Object>> selectRows() {
        return null;
    }

    public <T> List<T> selectRows(Class<T> cls) {
        return null;
    }

    public Map<String, Object> selectRow() {
        return null;
    }

    public <T> T selectRow(Class<T> cls) {
        return null;
    }

    public LocalDateTime selectDatetime() {
        return null;
    }

    public Long selectLong() {
        return null;
    }

    public List<Long> selectLongs() {
        return null;
    }

    public String selectString() {
        return null;
    }

    public Boolean selectBoolean() {
        return null;
    }
}
