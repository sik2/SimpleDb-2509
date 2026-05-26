package com.back.simpleDb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
        try {
            return DriverManager.getConnection(
                    "jdbc:mysql://" + localhost + ":3306/" + log,
                    root,
                    password
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public void setDevMode(boolean b) {
    }

    // 개별 인자로 전달하기 위한 오버로딩된 run 메서드
    // Object... params는 가변인자로, 여러 개의 파라미터를 전달할 수 있도록 해줍니다.
    // sql을 제외한 추가 매개변수가 없어도 작동
    public void run(String sql, Object... params) {
        try {
            Connection connection = getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            for (int i=0; i<params.length;i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
