package com.elice.cinema.domain.movie.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MovieStatus {

    UPCOMING("개봉예정"),
    NOW_SHOWING("상영중"),
    ENDED("상영종료");

    private final String displayName;
}
