package com.elice.cinema.domain.movie.entity;

import lombok.Getter;

@Getter
public enum AgeRating {

    ALL("전체 관람가"),
    AGE_12("12세 이상"),
    AGE_15("15세 이상"),
    AGE_19("19세 이상");

    private final String description;

    AgeRating(String description) {
        this.description = description;
    }
}
