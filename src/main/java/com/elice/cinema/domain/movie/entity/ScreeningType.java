package com.elice.cinema.domain.movie.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScreeningType {

    TWO_D("2D"),
    THREE_D("3D"),
    FOUR_D("4D"),
    IMAX("IMAX");

    private final String displayName;
}