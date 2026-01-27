package com.elice.cinema.domain.movie.dto.request;

import com.elice.cinema.domain.movie.entity.AgeRating;
import com.elice.cinema.domain.movie.entity.Genre;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class AdminMovieSearchRequest {

    // 키워드 검색
    private String keyword;

    // 다중 선택 필터
    private List<MovieStatus> statuses;
    private List<AgeRating> ageRatings;
    private List<Genre> genres;

    // 개봉/종료 기간 필터
    private LocalDate releaseStartDate;
    private LocalDate releaseEndDate;

    // 관리자용 정렬 정책
    private AdminMovieSortType sortType;
}