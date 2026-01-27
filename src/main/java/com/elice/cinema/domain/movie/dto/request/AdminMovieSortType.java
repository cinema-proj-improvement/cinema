package com.elice.cinema.domain.movie.dto.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminMovieSortType {

    RELEASE_DATE_DESC("최신순"),      // 개봉일 최신순
    END_DATE_DESC("종료일순"),          // 종료일 최신순
    AVG_SCORE_DESC("평점순"),         // 평점 높은 순
    RESERVATION_RATE_DESC("예매율순");  // 예매율 높은 순

    private final String displayName;

}
