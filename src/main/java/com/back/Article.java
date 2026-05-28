package com.back;

import java.time.LocalDateTime;

public class Article {
    private long id;
    private String title;
    private String body;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private boolean isBlind;

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public LocalDateTime getModifiedDate() { return modifiedDate; }
    public boolean isBlind() { return isBlind; }
}