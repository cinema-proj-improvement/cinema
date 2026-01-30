package com.elice.cinema.domain.movie.repository;

import com.elice.cinema.domain.movie.dto.request.AdminMovieSearchRequest;
import com.elice.cinema.domain.movie.dto.request.AdminMovieSortType;
import com.elice.cinema.domain.movie.dto.response.MovieListResponse;
import com.elice.cinema.domain.movie.entity.AgeRating;
import com.elice.cinema.domain.movie.entity.Genre;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.elice.cinema.domain.movie.entity.QMovie.movie;
import static com.elice.cinema.domain.movieImage.entity.QMovieImage.movieImage;

@RequiredArgsConstructor
public class MovieRepositoryImpl implements MovieRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    // 관리자 ID 페이징 조회
    @Override
    public List<Long> findAdminMovieIds(
            AdminMovieSearchRequest search, Pageable pageable) {
        return queryFactory
                .select(movie.id)
                .from(movie)
                .where(adminSearchConditions(search))
                .orderBy(resolveAdminSort(search.getSortType()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }
    @Override
    public long countAdminMovies(AdminMovieSearchRequest search) {
        return Optional.ofNullable(
                queryFactory
                        .select(movie.id.count())
                        .from(movie)
                        .where(adminSearchConditions(search))
                        .fetchOne()
        ).orElse(0L);
    }

    // 사용자 목록
    @Override
    public Page<MovieListResponse> findUserMovies(String keyword, String sort, Pageable pageable) {

        BooleanExpression condition = userVisibleCondition()
                .and(titleContains(keyword));

        List<MovieListResponse> content = queryFactory
                .select(Projections.constructor(
                        MovieListResponse.class,
                        movie.id,
                        movieImage.imageUrl,
                        movie.title,
                        movie.releaseDate,
                        movie.advanceReservationRate,
                        movie.avgScore
                ))
                .from(movie)
                .leftJoin(movieImage)
                .on(
                        movieImage.movie.eq(movie)
                                .and(movieImage.displayOrder.eq(0))
                )
                .where(condition)
                .orderBy(getUserSortOrder(sort))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(movie.id.count())
                .from(movie)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(
                content,
                pageable,
                total == null ? 0 : total
        );
    }

    // 사용자 상세 조회
    @Override
    public Optional<Movie> findUserMovieById(Long movieId) {

        return Optional.ofNullable(
                queryFactory
                        .selectFrom(movie)
                        .where(
                                movie.id.eq(movieId),
                                userVisibleCondition()
                        )
                        .fetchOne()
        );
    }

    // 사용자 종료 영화 제외
    private BooleanExpression userVisibleCondition() {
        return movie.status.in(MovieStatus.UPCOMING, MovieStatus.NOW_SHOWING);
    }

    // 사용자 정렬 분기 (예매율순 / 기본: 개봉일순)
    private OrderSpecifier<?> getUserSortOrder(String sort) {
        if ("reservationRate".equals(sort)) {
            return movie.advanceReservationRate.desc();
        }
        // 기본값: 개봉일 최신순
        return movie.releaseDate.desc();
    }


    // 관리자 검색 조건 묶음(상태,등급,장르,키워드,기간)
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
        return StringUtils.hasText(keyword)
                ? movie.title.containsIgnoreCase(keyword)
                : null;
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
            case RELEASE_DATE_DESC -> movie.releaseDate.desc();
            case END_DATE_DESC -> movie.endDate.desc();
            case AVG_SCORE_DESC -> movie.avgScore.desc();
            case RESERVATION_RATE_DESC -> movie.advanceReservationRate.desc();
        };
    }
}