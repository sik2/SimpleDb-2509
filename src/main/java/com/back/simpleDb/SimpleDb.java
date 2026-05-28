package com.back.simpleDb;

import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SimpleDb {
    private final String localhost;
    private final String root;
    private final String password;
    private final String log;

    private Connection connection;

    public SimpleDb(String localhost, String root, String password, String log) {
        this.localhost = localhost;
        this.root = root;
        this.password = password;
        this.log = log;
    }

    public Connection getConnection() {
        try {
            // 기존 커넥션 재사용 ( 커넥션이 있고 사용중일 때 )
            if (connection != null && !connection.isClosed()) {
                return connection;
            }

            // 신규 커넥션 생성
            connection =  DriverManager.getConnection(
                    // 시간대 설정 추가, MySQL 서버의 시간대와 애플리케이션의 시간대를 맞추기 위해 사용
                    "jdbc:mysql://" + localhost + ":3306/" + log + "?sessionVariables=time_zone='%2B09:00'",
                    root,
                    password
            );

            return  connection;

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

    @SneakyThrows
    public void startTransaction() {
        // JDBC는 자동 커밋이 되기 때문에 AutoCommit을 꺼서 자동 커밋이 안 되도록 함
        Connection c = getConnection();
        c.setAutoCommit(false);
    }

    @SneakyThrows
    public void commit() {
        // 수동으로 커밋을 진행하고, 기존 연결을 재사용함
        Connection c = getConnection();
        c.commit();
    }

    @SneakyThrows
    public void rollback() {
        // 수동으로 롤백을 진행하고, 기존 연결을 재사용함
        Connection c = getConnection();
        c.rollback();
    }
}
