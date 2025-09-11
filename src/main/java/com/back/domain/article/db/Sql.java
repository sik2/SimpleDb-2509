package com.back.domain.article.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Sql {
    private final SimpleDb simpleDb;
    private final StringBuilder rawSql;
    private final List<Object> params;

    Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
        this.rawSql = new StringBuilder();
        this.params = new ArrayList<>();
    }

    // SQL 문장과 파라미터를 추가하는 메서드
    public Sql append(String sql, Object... params) {
        rawSql.append(sql).append(" ");
        //받은 파라미터들을 List에 추가
        for (Object param : params) {
            this.params.add(param);
        }
        return this;
    }

    /**
     * INSERT, UPDATE, DELETE 등 데이터 변경 쿼리를 실행하고
     * 영향받은 row의 개수를 반환합니다.
     */
    public int update() {
        // try-with-resources 구문으로 Connection과 PreparedStatement 자원을 자동 해제
        try (Connection conn = DriverManager.getConnection(simpleDb.url, simpleDb.user, simpleDb.password);
             PreparedStatement pstmt = conn.prepareStatement(rawSql.toString().trim())) {

            // SQL의 '?' 부분에 파라미터를 순서대로 바인딩
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            // SQL을 실행하고, 영향받은 row의 개수를 반환
            return pstmt.executeUpdate();

        } catch (SQLException e) {
            // 예외 발생 시 RuntimeException으로 전환하여 처리 중단
            throw new RuntimeException(e);
        }
    }
}
