package com.back.simpleDb;

import com.back.domain.Article;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sql {

    private final SimpleDb simpleDb;
    private final StringBuilder stringBuilder;
    private final List<Object> params;

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
        this.stringBuilder = new StringBuilder();
        this.params = new ArrayList<>();
    }


    public Sql append(String query, Object... args) {
        stringBuilder.append(query).append("\n");

        for (Object arg : args) {
            params.add(arg);
        }

        return this;
    }

    // 다중행 비교 (IN)
    public Sql appendIn(String query, Object... args) {
        return this;
    }

    private PreparedStatement prepareStatement(Connection conn) throws SQLException {
        return prepareStatement(conn, false);
    }

    private PreparedStatement prepareStatement(Connection conn, boolean returnGeneratedKeys) throws SQLException {
        String sql = stringBuilder.toString();
        PreparedStatement pstmt = returnGeneratedKeys ?
                conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS) : conn.prepareStatement(sql);

        for (int i = 0; i < params.size(); i++) {
            pstmt.setObject(i + 1, params.get(i));
        }
        return pstmt;
    }

    private int executeUpdate() {
        Connection conn = simpleDb.getConnection();

        try (PreparedStatement pstmt = prepareStatement(conn)) {
            return pstmt.executeUpdate();
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }

    }

    // 데이터베이스 입력 메서드
    public long insert() {
        Connection conn = simpleDb.getConnection();

        try (PreparedStatement pstmt = prepareStatement(conn, true)){
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()){
                if(rs.next()) {
                    return rs.getLong(1);
                }
            return 0;
            } catch(SQLException e) {
                throw new RuntimeException(e);
            }
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }

    }


    public int update() {
        return executeUpdate();
    }

    public int delete() {
        return executeUpdate();
    }

    //여러 값 출력
    public List<Map<String, Object>> selectRows() {
        Connection conn = simpleDb.getConnection();

        try(PreparedStatement pstmt = prepareStatement(conn); ResultSet rs = pstmt.executeQuery()) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<Map<String, Object>> rows = new ArrayList<>();


            while(rs.next()) {
                Map<String, Object> row = new HashMap<>();

                for(int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);

                    row.put(columnName, value);
                }
                rows.add(row);
            }

            return rows;

        } catch(SQLException e) {
            throw new RuntimeException();
        }

    }


    public List<Article> selectRows(Class<Article> aritcle) {
        return null;
    }

    // 단건 출력
    public Map<String, Object> selectRow() {
        Connection conn = simpleDb.getConnection();

        try(PreparedStatement pstmt = prepareStatement(conn); ResultSet rs = pstmt.executeQuery()) {


            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            if(rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for(int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                return row;
            }

            return null;

        } catch(SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public Article selectRow(Class<Article> article) {
        return null;
    }

    //시간 출력
    public LocalDateTime selectDatetime() {
        return null;
    }

    // 아이디 등 숫자 출력?
    public long selectLong() {
        return 0;
    }

    // 다중 건 출력
    public List<Long> selectLongs() {
        return null;
    }

    public String selectString() {
        return null;
    }

    public boolean selectBoolean() {
        return false;
    }


}
