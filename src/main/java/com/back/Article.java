package com.back;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Article {

    private long id;
    private String title;
    private String Body;
    private LocalDateTime CreatedDate;
    private LocalDateTime ModifiedDate;
    private boolean isBlind;

}
