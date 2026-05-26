package com.back.simpleDb;

import java.sql.Connection;

public class SimpleDb {
    private final String localhost;
    private final String root;
    private final String password;
    private final String log;

    public SimpleDb(String localhost, String root, String password, String log) {
        this.localhost = localhost;
        this.root = root;
        this.password = password;
        this.log = log;
    }

    public Connection getConnection() {
        return null;
    }

    public void setDevMode(boolean b) {
    }

    public void run(String sql) {
    }

    // 개별 인자로 전달하기 위한 오버로딩된 run 메서드
    public void run(String sql, Object... params) {
    }

    public Sql genSql() {
        return new Sql(this);
    }

    public void close() {
    }

    public void startTransaction() {
    }

    public void commit() {
    }

    public void rollback() {
    }
}
