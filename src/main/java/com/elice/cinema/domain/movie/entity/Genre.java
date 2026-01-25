package com.elice.cinema.domain.movie.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Genre {
    ACTION("액션"),
    DRAMA("드라마"),
    COMEDY("코미디"),
    ROMANCE("로맨스"),
    THRILLER("스릴러"),
    HORROR("공포"),
    FANTASY("판타지"),
    SCI_FI("SF"),
    ANIMATION("애니메이션"),
    DOCUMENTARY("다큐멘터리"),
    CRIME("범죄"),
    MYSTERY("미스터리"),
    ADVENTURE("어드벤처"),
    FAMILY("가족"),
    WAR("전쟁"),
    MUSIC("음악");

    private final String displayName;
}