package com.elice.cinema.domain.movie.dto.response;

import com.elice.cinema.domain.movie.entity.AgeRating;
import com.elice.cinema.domain.movie.entity.Genre;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

/**
 * QueryDSL constructor projection 전용 DTO
 * - 필드 순서 = 쿼리 select 순서
 * - 순서 변경 시 QueryDSL 쿼리도 반드시 함께 수정해야 함
 */


@Getter
@AllArgsConstructor
public class AdminMovieJoinRowResponse {
    // QueryDSL constructor projection 전용 row DTO
    private final Long movieId;
    private final String thumbnail;
    private final String title;
    private final Genre genre; // JOIN 결과의 row 단위 장르 값 (중복은 DB/조회 단계에서 이미 제거됨)
    private final MovieStatus status;
    private final AgeRating ageRating;
    private final LocalDate releaseDate;
    private final LocalDate endDate;
    private final Double avgScore;
    private final Double advanceReservationRate;

    public Double getAdvanceReservationRate() {
        if (advanceReservationRate == null) {
            return null;
        }
        return Math.round(advanceReservationRate * 10) / 10.0;
    }
}
