package com.back.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import lombok.Setter;

public class SimpleDb {

    private final String host;
    private final String userName;
    private final String password;
    private final String database;
    @Setter
    private boolean devMode;

    private static final int PORT = 3306;

    public SimpleDb(String host, String userName, String password, String database) {
        this.host = host;
        this.userName = userName;
        this.password = password;
        this.database = database;
    }

    // TODO : thread를 이용한 커넥션 풀을 통한 커넥션 획득
    private Connection getConnection() {
        String URL = "jdbc:mysql://" + host + ":" + PORT + "/" + database;
        try {
            return DriverManager.getConnection(URL, userName, password);
        } catch (SQLException e) {
            // 코드 간결성을 위한 unchecked 예외로 변환
            throw new RuntimeException(e);
        }
    }

    public void run(String sql) {

    }

    public void run(String sql, Object... args) {

    }


    public Sql genSql() {
        return null;
    }

    public void close() {

    }

    public void startTransaction() {

    }

    public void rollback() {
    }

    public void commit() {

    }
}
