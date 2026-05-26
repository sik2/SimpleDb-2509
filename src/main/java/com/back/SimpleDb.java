package com.back;

import com.back.simpleDb.Sql;
import lombok.Setter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SimpleDb {
    private final String host;
    private final String user;
    private final String password;
    private final String dbName;
    @Setter
    private boolean devMode;

    private final ThreadLocal<Connection> connectionThreadLocal = new ThreadLocal<>();

    public SimpleDb(String host, String user, String password, String dbName)
    {
        this.host = host;
        this.user = user;
        this.password = password;
        this.dbName = dbName;
    }

    public Connection getconnection(){
        Connection conn = connectionThreadLocal.get();
        try{
            if(conn == null || conn.isClosed())
            {
                String uri = "jdbc:mysql://" + host + ":3307/" + dbName
                        + "?serverTimeZone=Asia/Seoul";
                conn = DriverManager.getConnection(uri, user, password);
                connectionThreadLocal.set(conn);
            }
            return conn;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close(){
        Connection conn = connectionThreadLocal.get();
        if(conn != null){
            try{
                conn.close();
            } catch (SQLException e) {
                connectionThreadLocal.remove();
            }
        }
    }

    public void run(String sql, Object... params) {
        try(PreparedStatement ps = getconnection().prepareStatement(sql))
        {
            for(int i=0; i< params.length; i++)
            {
                ps.setObject(i+1, params[i]);
            }
            if(devMode) System.out.println("== raw Sql ==\n %s".formatted(sql));
            ps.execute();
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public Sql genSql(){
        return new Sql(this,devMode);
    }

    public void startTransaction() {
        try{
            getconnection().setAutoCommit(false);
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public void rollback() {
        try{
            getconnection().rollback();
            getconnection().setAutoCommit(true);
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        try{
            getconnection().commit();
            getconnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
