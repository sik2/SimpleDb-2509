package com.back;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Article {
    private long id;
    private String title;
    private String body;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private boolean isBlind;

//    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
//    PRIMARY KEY(id),
//    createdDate DATETIME NOT NULL,
//    modifiedDate DATETIME NOT NULL,
//    title VARCHAR(100) NOT NULL,
//    `body` TEXT NOT NULL,
//    isBlind BIT(1) NOT NULL DEFAULT 0
}
