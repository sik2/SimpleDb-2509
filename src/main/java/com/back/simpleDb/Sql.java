package com.back.simpleDb;

import com.back.SimpleDb;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

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
        return (long) submit();
    }

    public int update() {
        return (int) submit();
    }

    public int delete() {
        return (int) submit();
    }

    private Object submit()
    {
        if(devMode) System.out.println("== raw Sql ==\n %s".formatted(query));

        String sqlUpper = query.toString().trim().toUpperCase();

        try{
            if(sqlUpper.startsWith("INSERT"))
            {
                try(PreparedStatement ps = simpleDb.getconnection()
                                            .prepareStatement(query.toString(),Statement.RETURN_GENERATED_KEYS))
                {
                    bindParams(ps);
                    ps.execute();
                    ResultSet rs = ps.getGeneratedKeys();
                    rs.next();
                    return rs.getLong(1);
                }

            }else{
                try(PreparedStatement ps = simpleDb.getconnection()
                                            .prepareStatement(query.toString()))
                {
                    bindParams(ps);
                    return ps.executeUpdate();
                }
            }
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    private void bindParams(PreparedStatement ps) throws SQLException {
        for(int i=0; i< params.size(); i++)
        {
            ps.setObject(i+1, params.get(i));
        }
    }

    public List<Map<String, Object>> selectRows() {
        if(devMode) System.out.println("== raw Sql ==\n %s".formatted(query));

        try{
            PreparedStatement ps = simpleDb.getconnection().prepareStatement(query.toString());

            bindParams(ps);
            ResultSet rs = ps.executeQuery();

            ResultSetMetaData meta = rs.getMetaData();
            int columCount = meta.getColumnCount();

            List<Map<String, Object>> rows = new ArrayList<>();


            while (rs.next())
            {
                 Map<String,Object> row = new LinkedHashMap<>();
                 for(int i=1; i<= columCount; i++)
                 {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                 }

                 rows.add(row);
            }

            return rows;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> selectRow() {
        if(devMode) System.out.println("== raw Sql ==\n %s".formatted(query));

        try{
            PreparedStatement ps = simpleDb.getconnection().prepareStatement(query.toString());

            bindParams(ps);
            ResultSet rs = ps.executeQuery();

            ResultSetMetaData meta = rs.getMetaData();
            int columCount = meta.getColumnCount();

            Map<String,Object> row = new LinkedHashMap<>();

            while (rs.next())
            {
                for(int i=1; i<= columCount; i++)
                {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
            }

            return row;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public LocalDateTime selectDatetime() {
        return LocalDateTime.now();
    }

    public Long selectLong() {
        if(devMode) System.out.println("== raw Sql ==\n %s".formatted(query));

        try{
            PreparedStatement ps = simpleDb.getconnection().prepareStatement(query.toString());

            bindParams(ps);
            ResultSet rs = ps.executeQuery();

            rs.next();

            return rs.getLong(1);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
