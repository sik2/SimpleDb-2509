package com.back.domain.article.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SimpleDb {
    // DB 접속 정보를 저장하는 필드 (package-private으로 변경하여 Sql 클래스에서 접근 가능하도록 수정)
    final String url;
    final String user;
    final String password;
    // 개발 모드 활성화 여부
    private boolean devMode;

    // 생성자: DB 접속 정보를 받아 필드를 초기화합니다.
    public SimpleDb(String host, String user, String password, String dbName) {
        this.url = "jdbc:mysql://" + host + "/" + dbName + "?useUnicode=true&characterEncoding=utf8&autoReconnect=true&serverTimezone=Asia/Seoul";
        this.user = user;
        this.password = password;
        this.devMode = false;
    }

    // 개발자 모드를 설정하는 메서드
    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    // 개발 모드인지 확인하는 내부용(package-private) 메서드
    boolean isDevMode() {
        return this.devMode;
    }

    //결과값이 필요 없는 단순 SQL(CREATE, INSERT, TRUNCATE 등)을 실행하는 편의 메서드입니다.
    public void run(String sql, Object... params) {
        // 내부적으로 Sql 객체를 생성하고, append와 update를 연속으로 호출하여 SQL을 실행합니다.
        new Sql(this).append(sql, params).update();
    }

    public Sql genSql() {
        return new Sql(this);
    }
}
