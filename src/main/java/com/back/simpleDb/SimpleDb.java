package com.back.simpleDb;

import lombok.Getter;
import lombok.Setter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;



@Getter
@Setter
public class SimpleDb {

    private final String serverName;
    private final String username;
    private final String password;
    private final String schemaName;
    private boolean devMode = false;


    public SimpleDb(String serverName, String username, String password, String schemaName) {
        this.serverName = serverName;
        this.username = username;
        this.password = password;
        this.schemaName = schemaName;
    }

    public String run(String sql, Object... args) {
        Connection conn = getConnection();
        try(PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for(int i = 0; i < args.length; i++) {
                pstmt.setObject(i + 1, args[i]);
            }
            pstmt.executeUpdate();
            return sql;
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }


    }

    public Connection getConnection() {
        try {
            String url = "jdbc:mysql://%s:3306/%s".formatted(serverName, schemaName);
            return DriverManager.getConnection(url, username, password);
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Sql genSql() {
        return new Sql(this);
    }


    public void close() {

    }


    // 트랜잭션 관련 메서드
    public void startTransaction() {

    }

    public void rollback() {

    }

    public void commit() {

    }






}
