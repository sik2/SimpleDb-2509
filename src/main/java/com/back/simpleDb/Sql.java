package com.back.simpleDb;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Sql {

    private final SimpleDb db;
    StringBuffer querySb = new StringBuffer();
    List<Object> args = new ArrayList<Object>();

    public Sql(SimpleDb db) {
        this.db = db;
    }

    public Sql append(String sql, Object... args) {
        querySb.append(sql).append(" ");
        if(args != null && args.length > 0) {
            this.args.addAll(Arrays.asList(args));
        }
        return this;
    }

    public long insert() {
        return db.insert(querySb.toString(), args.toArray());
    }

    public int update() {
        return db.updateOrDelete(querySb.toString(), args.toArray());
    }

    public int delete() {
        return db.updateOrDelete(querySb.toString(), args.toArray());
    }

    public List<Map<String, Object>> selectRows() {
        return db.selectRows(querySb.toString(), args.toArray());
    }

    public Map<String, Object> selectRow() {
        return db.selectRow(querySb.toString(), args.toArray());
    }

    public LocalDateTime selectDatetime() {
        Map<String, Object> row = db.selectRow(querySb.toString(), args.toArray());
        if(row == null) {
            return null;
        }
        for(Object value : row.values()) {
            System.out.println(value.getClass().getName());
            if(value instanceof LocalDateTime) {
                return (LocalDateTime) value;
            }
        }
        return null;
    }

    public Long selectLong() {
        Map<String, Object> row = db.selectRow(querySb.toString(), args.toArray());
        if(row == null) {
            return null;
        }
        for(Object value : row.values()) {
            System.out.println(value.getClass().getName());
            if(value instanceof Long) {
                return (Long) value;
            }
        }
        return null;
    }

    public String selectString() {
        Map<String, Object> row = db.selectRow(querySb.toString(), args.toArray());
        if(row == null) {
            return null;
        }
        for(Object value : row.values()) {
            System.out.println(value.getClass().getName());
            if(value instanceof String) {
                return (String) value;
            }
        }
        return null;
    }

    public Boolean selectBoolean() {
        Map<String, Object> row = db.selectRow(querySb.toString(), args.toArray());
        if(row == null) {
            return null;
        }
        for(Object value : row.values()) {
            System.out.println(value.getClass().getName());
            if(value instanceof Boolean) {
                return (Boolean) value;
            }
        }
        return null;
    }
}
