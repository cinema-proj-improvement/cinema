package com.elice.cinema.domain.movie.repository;

import com.elice.cinema.domain.movie.dto.request.AdminMovieSearchRequest;
import com.elice.cinema.domain.movie.dto.request.AdminMovieSortType;
import com.elice.cinema.domain.movie.entity.AgeRating;
import com.elice.cinema.domain.movie.entity.Genre;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static com.elice.cinema.domain.movie.entity.QMovie.movie;

@RequiredArgsConstructor
public class MovieRepositoryImpl implements MovieRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Movie> findAdminMovieList(
            AdminMovieSearchRequest search,
            Pageable pageable
    ) {

        List<Movie> content = queryFactory
                .selectFrom(movie)
                .where(adminSearchConditions(search))
                .orderBy(resolveAdminSort(search.getSortType()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(movie.id.count())
                .from(movie)
                .where(adminSearchConditions(search))
                .fetchOne();

        return new PageImpl<>(
                content,
                pageable,
                total == null ? 0 : total
        );
    }

    private BooleanExpression[] adminSearchConditions(AdminMovieSearchRequest search) {
        return new BooleanExpression[]{
                statusIn(search.getStatuses()),
                ageRatingIn(search.getAgeRatings()),
                genreIn(search.getGenres()),
                titleContains(search.getKeyword()),
                releasePeriodOverlaps(
                        search.getReleaseStartDate(),
                        search.getReleaseEndDate()
                )
        };
    }

    // 필터 조건
    private BooleanExpression statusIn(List<MovieStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        return movie.status.in(statuses);
    }

    private BooleanExpression ageRatingIn(List<AgeRating> ageRatings) {
        if (ageRatings == null || ageRatings.isEmpty()) {
            return null;
        }
        return movie.ageRating.in(ageRatings);
    }

    private BooleanExpression genreIn(List<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return null;
        }
        return movie.genres.any().in(genres);
    }

    private BooleanExpression titleContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return movie.title.containsIgnoreCase(keyword);
    }

    // 검색 기간 영화개봉-종료 하루라도 겹치면 포함
    private BooleanExpression releasePeriodOverlaps(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate != null && endDate != null) {
            return movie.releaseDate.loe(endDate)
                    .and(movie.endDate.goe(startDate));
        }

        if (startDate != null) {
            return movie.endDate.goe(startDate);
        }

        if (endDate != null) {
            return movie.releaseDate.loe(endDate);
        }

        return null;
    }

    // 정렬
    private OrderSpecifier<?> resolveAdminSort(AdminMovieSortType sortType) {

        if (sortType == null) {
            return movie.createdAt.desc();
        }

        return switch (sortType) {
            case RELEASE_DATE_DESC ->
                    movie.releaseDate.desc();
            case END_DATE_DESC ->
                    movie.endDate.desc();
            case AVG_SCORE_DESC ->
                    movie.avgScore.desc();
            case RESERVATION_RATE_DESC ->
                    movie.advanceReservationRate.desc();
        };
    }
}