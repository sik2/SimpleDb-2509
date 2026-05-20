package com.back.simpleDb;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record Sql(SimpleDb simpleDb) {

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

    public Map<String, Object> selectRow() {
        return null;
    }

    public <T> List<T> selectRows(Class<T> cls) {
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
