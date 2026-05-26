package com.back.simpleDb;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class Sql {
    private final SimpleDb simpleDb;

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sql, Object... params) {
        return this;
    }

    public Sql appendIn(String sql, Object... params) {
        return this;
    }

    public long insert() {
        return 0;
    }

    public int update() {
        return 0;
    }

    public int delete() {
        return 0;
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