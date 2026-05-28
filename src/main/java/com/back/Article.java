package com.back;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class Article {
    private long id;
    private String title;
    private String body;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;

    @JsonProperty("isBlind")
    private boolean isBlind;

    public Article() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(LocalDateTime modifiedDate) { this.modifiedDate = modifiedDate; }

    public boolean isBlind() { return isBlind; }

    @JsonProperty("isBlind")
    public void setIsBlind(boolean isBlind) { this.isBlind = isBlind; }
}