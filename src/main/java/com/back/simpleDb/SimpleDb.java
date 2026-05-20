package com.back.simpleDb;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimpleDb {
    private final String host;
    private final String username;
    private final String password;
    private final String databaseName;
    private boolean devMode = false;

    public SimpleDb(String host, String username, String password, String databaseName) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.databaseName = databaseName;
    }

    public Sql genSql() {
        return new Sql(this);
    }

    public void run(String sql, Object... params) {
    }

    public void startTransaction() {
    }

    public void commit() {
    }

    public void rollback() {
    }

    public void close() {
    }

}
