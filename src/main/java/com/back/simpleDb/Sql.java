package com.back.simpleDb;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Sql {
    private final SimpleDb simpleDb;
    private String sql = "";
    private final List<Object> params = new ArrayList<>();

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }
    public Sql append(String sql, Object... params) {
        this.sql += sql + " ";
        if (params.length != 0)
            this.params.addAll(Arrays.asList(params));

        return this;
    }

    public Sql appendIn(String sql, Object... params) {
        String placeholders = String.join(", ", java.util.Collections.nCopies(params.length, "?"));

        this.sql += sql.replace("?", placeholders) + " ";
        this.params.addAll(Arrays.asList(params));

        return this;
    }

    public long insert() {
        long insert =  simpleDb.insert(sql, params.toArray());
        sql = "";
        return insert;
    }

    public int update() {
        int update = simpleDb.runForRowsCount(sql, params.toArray());
        sql = "";
        return update;
    }

    public int delete() {
        int delete = simpleDb.runForRowsCount(sql, params.toArray());
        sql = "";
        return delete;
    }

    public List<Map<String, Object>> selectRows() {
        List<Map<String, Object>> selectedRows = simpleDb.runForRows(sql, params.toArray());
        sql = "";
        return selectedRows;
    }

    public Map<String, Object> selectRow() {
        Map<String, Object> selectedRow = simpleDb.runForRows(sql, params.toArray()).getFirst();
        sql = "";
        return selectedRow;
    }

    public <T> List<T> selectRows(Class<T> cls) {
        sql = "";
        return null;
    }

    public <T> T selectRow(Class<T> cls)
    {
        sql = "";
        return null;
    }

    public LocalDateTime selectDatetime() {
        LocalDateTime selectDatetime = simpleDb.selectOne(sql, LocalDateTime.class, params.toArray());
        sql = "";
        return selectDatetime;
    }

    public Long selectLong() {
        Long id = simpleDb.selectOne(sql, Long.class, params.toArray());
        sql = "";
        return id;
    }

    public List<Long> selectLongs() {
        sql = "";

        return null;
    }

    public String selectString() {
        String string = simpleDb.selectOne(sql, String.class, params.toArray());

        sql = "";
        return string;
    }

    public Boolean selectBoolean() {
        Boolean aBoolean = simpleDb.selectOne(sql, Boolean.class, params.toArray());

        sql = "";
        return aBoolean;
    }
}
