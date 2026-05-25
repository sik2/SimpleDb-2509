package com.back.simpleDb;

public class SimpleDb {

    public SimpleDb(String host, String username, String password, String database) {
    }

    public void setDevMode(boolean devMode) {
    }

    public void run(String sql, Object... params) {
    }

    public Sql genSql() {
        return new Sql();
    }

    public void startTransaction() {
    }

    public void rollback() {
    }

    public void commit() {
    }

    public void close() {
    }
}