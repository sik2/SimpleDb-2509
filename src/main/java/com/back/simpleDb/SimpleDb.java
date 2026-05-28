package com.back.simpleDb;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Getter
@Setter
@RequiredArgsConstructor
public class SimpleDb {
    private final String host, user, password, dbName;
    private boolean devMode;
    private ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    // 스레드별 Connection 가져오기 (없으면 새로 생성)
    public Connection getConnection() {
        Connection conn = connectionHolder.get();
        try {
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(
                        "jdbc:mysql://%s/%s?serverTimezone=Asia/Seoul".formatted(host, dbName),
                        user,
                        password
                );
                connectionHolder.set(conn);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return conn;
    }

    // 스레드별 Connection 닫기
    public void close() {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                connectionHolder.remove();
            }
        }
    }
    // 파라미터 없는 SQL 실행
    public void run(String sql) {
        run(sql, new Object[0]);
    }

    // 파라미터 있는 SQL 실행
    public void run(String sql, Object... params) {
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]); //ps.setObject(index, value)
            }
            if (devMode) {
                System.out.println("== rawSql ==");
                System.out.println(sql);
                System.out.println("params: " + java.util.Arrays.toString(params));
            }
            ps.execute();   //SQL 실행
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Sql genSql(){
        return new Sql(this);
    }

    public void startTransaction() {
        try{
            getConnection().setAutoCommit(false);   //자동 커밋 끄기
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void rollback() {
        try{
            Connection conn = getConnection();
            conn.rollback();
            conn.setAutoCommit(true); // 자동 커밋 다시 켜기
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        try {
            Connection conn = getConnection();
            conn.commit();
            conn.setAutoCommit(true); // 자동 커밋 다시 켜기
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
