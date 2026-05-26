package com.back;

import lombok.Data;

import java.time.LocalDateTime;

@Data // Lombok: getter, setter, toString, equals, hashCode 자동 생성
public class Article {
    private long id;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private String title;
    private String body;
    private boolean isBlind;
}
