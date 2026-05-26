package com.back.simpleDb;


public class SimpleDb {
    private final String host;
    private final String user;
    private final String password;
    private final String database;

    public SimpleDb(String host, String username, String password, String database) {
        this.host = host;
        this.user = username;
        this.password = password;
        this.database = database;
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