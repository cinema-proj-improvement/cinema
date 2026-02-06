package com.elice.cinema.domain.movie.repository;

import com.elice.cinema.domain.movie.dto.request.AdminMovieSortType;
import com.elice.cinema.domain.movie.dto.response.AdminMovieJoinRowResponse;
import com.elice.cinema.domain.movie.entity.Genre;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.elice.cinema.domain.movie.entity.QMovie.movie;
import static com.elice.cinema.domain.movieImage.entity.QMovieImage.movieImage;

@Repository
@RequiredArgsConstructor
public class AdminMovieJoinQueryRepositoryImpl implements AdminMovieJoinQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<AdminMovieJoinRowResponse> findAdminMovieJoinRows(
            List<Long> movieIds,
            AdminMovieSortType sortType
    ) {

        EnumPath<Genre> genre = Expressions.enumPath(Genre.class, "genre");

        var query = queryFactory
                .select(Projections.constructor(
                        AdminMovieJoinRowResponse.class,
                        movie.id,
                        movieImage.imageUrl,
                        movie.title,
                        genre,
                        movie.status,
                        movie.ageRating,
                        movie.releaseDate,
                        movie.endDate,
                        movie.avgScore,
                        movie.advanceReservationRate
                ))
                .from(movie)
                .leftJoin(movieImage)
                .on(
                        movieImage.movie.eq(movie)
                                .and(movieImage.displayOrder.eq(0))
                )
                .leftJoin(movie.genres, genre)
                .where(movie.id.in(movieIds));

        switch (sortType) {
            case RELEASE_DATE_DESC ->
                    query.orderBy(movie.releaseDate.desc());
            case END_DATE_DESC ->
                    query.orderBy(movie.endDate.desc());
            case AVG_SCORE_DESC ->
                    query.orderBy(movie.avgScore.desc().nullsLast());
            case RESERVATION_RATE_DESC ->
                    query.orderBy(movie.advanceReservationRate.desc().nullsLast());
        }

        return query.fetch();
    }
}
