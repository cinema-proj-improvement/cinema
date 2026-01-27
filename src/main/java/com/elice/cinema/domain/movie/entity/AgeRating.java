package com.elice.cinema.domain.movie.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AgeRating {

    ALL("전체 관람가"),
    AGE_12("12세 이상 관람가"),
    AGE_15("15세 이상 관람가"),
    AGE_19("청소년 관람불가");

    private final String displayName;
}
