package com.back.simpleDb;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Sql {
    // SQL 조각과 파라미터를 저장한다.
    private List<String> sqlBits = new ArrayList<>();
    private List<Object> params = new ArrayList<>();

    private SimpleDb simpleDb;

    // SQL 조각과 파라미터를 누적한다.
    public Sql append(String sqlBit, Object... params) {
        sqlBits.add(sqlBit);

        for(Object parm : params){
            this.params.add(parm);
        }
        return this;
    }

    public long insert() {
        //sql조각을 공백으로 합친다.
        String sql= String.join(" ",sqlBits);

        // DB에 연결해 INSERT를 실행하고 생성된 키를 받는다.
        try (
                Connection connection = simpleDb.getConnection();
                PreparedStatement preparedStatement =
                        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            for (int i = 0; i < params.size(); i++) {
                preparedStatement.setObject(i + 1, params.get(i));
            }

            preparedStatement.executeUpdate();

            try (ResultSet rs = preparedStatement.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

            return 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }
}
