package com.back.simpleDb;

import com.back.domain.Article;

import java.time.LocalDateTime;
import java.util.ArrayList;
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


        return this;
    }

    // 다중행 비교 (IN)
    public Sql appendIn(String query, Object... args) {
        return this;
    }

    public long insert() {
        return 0;
    }

    public int update() {
        return 0;
    }

    public int delete() {
        return 0;
    }

    //여러 값 출력
    public List<Map<String, Object>> selectRows() {
        return null;
    }


    public List<Article> selectRows(Class<Article> aritcle) {
        return null;
    }

    // 단건 출력
    public Map<String, Object> selectRow() {
        return null;
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
