package com.back.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Article {
    private long id;
    private String title;
    private String body;
    private boolean isBlind;

    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
}
