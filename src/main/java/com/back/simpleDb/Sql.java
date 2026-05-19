package com.back.simpleDb;

import com.back.SimpleDb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Sql {
    private final SimpleDb simpleDb;
    private boolean devMode;
    private final StringBuilder query = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    public Sql(SimpleDb simpleDb, boolean devMode) {
        this.simpleDb = simpleDb;
        this.devMode =devMode;
    }

    public Sql append(String sql, Object... args) {
        query.append(sql).append(" ");
        Collections.addAll(params, args);
        return this;
    }

    public long insert(){
        if(devMode) System.out.println("== raw Sql ==\n %s".formatted(query));

        PreparedStatement ps = null;
        try {
            ps = simpleDb.getconnection().
                    prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS);
            for(int i=0; i< params.size(); i++)
            {
                ps.setObject(i+1, params.get(i));
            }

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            return rs.getLong(1);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        finally {
            if(ps !=null)
            {
                try{
                    ps.close();
                }catch (SQLException e){
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public int update() {
        if(devMode) System.out.println("== raw Sql ==\n %s".formatted(query));

        PreparedStatement ps = null;
        try {
            ps = simpleDb.getconnection().
                    prepareStatement(query.toString());
            for(int i=0; i< params.size(); i++)
            {
                ps.setObject(i+1, params.get(i));
            }

            return ps.executeUpdate();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        finally {
            if(ps !=null)
            {
                try{
                    ps.close();
                }catch (SQLException e){
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public int delete() {
        return 0;
    }
}
