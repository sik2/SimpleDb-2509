package com.back.simpleDb;

import lombok.Setter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SimpleDb {

    private final String url;
    private final String username;
    private final String password;
    private final String dbName;
    @Setter
    private boolean devMode = false;

    // 각 스레드별로 독립적인 DB 세션(커넥션 및 트랜잭션 상태)을 저장하는 저장소
    private final ThreadLocal<ConnectionContext> connectionContext = ThreadLocal.withInitial(ConnectionContext::new);

    // 스레드별 상태를 담을 내부 클래스
    private static class ConnectionContext {
        Connection connection;
        boolean inTransaction = false;
    }

    public SimpleDb(String host, String username, String password, String dbName) {
        // MySQL 8.0+ 버전에 맞는 JDBC URL (타임존 설정 포함)
        this.url = String.format("jdbc:mysql://%s:3306/%s?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true", host, dbName);
        this.username = username;
        this.password = password;
        this.dbName = dbName;
    }

    public Connection getConnection() throws SQLException {
        ConnectionContext context = connectionContext.get();

        try {
            if(context.connection == null || context.connection.isClosed()) {
                context.connection = DriverManager.getConnection(url, username, password);
                // 자동 커밋 활성화
                context.connection.setAutoCommit(!context.inTransaction);
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB 연결 실패", e);
        }
        return context.connection;
    }

    public Sql genSql() {
        return new Sql(this);
    }

    public void run(String sql, Object... params) {
        try (Sql sqlObj = genSql().append(sql, params)) {
            if (sql.trim().toUpperCase().startsWith("SELECT")) {
                sqlObj.selectRows();
            } else if (sql.trim().toUpperCase().startsWith("INSERT")) {
                sqlObj.insert();
            } else if (sql.trim().toUpperCase().startsWith("UPDATE")) {
                sqlObj.update();
            } else if (sql.trim().toUpperCase().startsWith("DELETE") || sql.trim().toUpperCase().startsWith("TRUNCATE") || sql.trim().toUpperCase().startsWith("DROP") || sql.trim().toUpperCase().startsWith("CREATE")) {
                sqlObj.delete(); // 내부적으로 executeUpdate() 호출하는 메서드 공유 가능
            }
        }
    }

    // 트랜잭션 시작
    public void startTransaction() {
        ConnectionContext context = connectionContext.get();
        context.inTransaction = true;
        try {
            getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 커밋
    public void commit() {
        ConnectionContext context = connectionContext.get();
        try {
            if (context.connection != null && !context.connection.isClosed()) {
                context.connection.commit();
                context.connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            context.inTransaction = false;
        }
    }

    // 롤백
    public void rollback() {
        ConnectionContext context = connectionContext.get();
        try {
            if (context.connection != null && !context.connection.isClosed()) {
                context.connection.rollback();
                context.connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            context.inTransaction = false;
        }
    }

    // t017 멀티스레드 테스트에서 호출됨: 현재 스레드의 커넥션을 닫고 ThreadLocal 해제
    public void close() {
        ConnectionContext context = connectionContext.get();
        try {
            if (context.connection != null && !context.connection.isClosed()) {
                context.connection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            connectionContext.remove(); // 메모리 누수 방지를 위해 ThreadLocal 완전히 제거
        }
    }


}
