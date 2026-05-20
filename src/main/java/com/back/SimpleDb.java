package com.back;

import lombok.Getter;
import lombok.Setter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SimpleDb {
    private final String url;
    private final String username;
    private final String password;

    @Setter
    private boolean devMode = false;

    public SimpleDb(String host, String username, String password, String dbName ){
        this.url = "jdbc:mysql://" + host + ":3306/" + dbName + "?serverTimeZone=Asia/Seoul";
        this.username = username;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    public void run(String sql, Object... args) {
        if (devMode) {
            System.out.println("== rawSql ==");
            System.out.println(toCompleteSql(sql, args));
        }
        try{
            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            bindParams(pstmt, args);

            pstmt.executeUpdate();
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    protected void bindParams(PreparedStatement pstmt, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            pstmt.setObject(i + 1, args[i]);
        }
    }

    public String toCompleteSql(String sql, Object... args) {
        for (Object arg : args) {
            String value;
            if (arg instanceof String) {
                value = "'%s'".formatted(arg);
            } else if (arg instanceof Boolean) {
                value = (boolean) arg ? "1" : "0";
            } else {
                value = String.valueOf(arg);
            }

            sql = sql.replaceFirst("\\?", value);
        }

        return sql;
    }

    public Sql genSql() {
        return new Sql(this);
    }

    public void close() {
    }

    public void startTransaction() {
    }

    public void rollback() {
    }

    public void commit() {
    }

    public boolean isDevMode() {
        return devMode;
    }
}
