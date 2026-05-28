package com.back.simpleDb;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SimpleDb {
    //testcode에 SimpleDb 파라미터를 저장할 변수 선언
    private String host;
    private String userName;
    private String password;
    private String dbName;

    public void setDevMode(boolean b) {
    }

    public SimpleDb(String host, String userName, String password, String dbName) {
        this.host = host;
        this.userName = userName;
        this.password = password;
        this.dbName = dbName;
    }

    public Sql genSql() {
        return new Sql(this);
    }
    // DB 연결 메서드
    Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://%s:3306/%s".formatted(host, dbName);
        return DriverManager.getConnection(url, userName, password);
    }

    // SQL과 파라미터를 받아 DB에 실행하는 메서드
    public void run(String sql, Object... params) {
        try (
                Connection connection = getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)
        ) {
            for (int i = 0; i < params.length; i++) {
                preparedStatement.setObject(i + 1, params[i]);
            }

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
