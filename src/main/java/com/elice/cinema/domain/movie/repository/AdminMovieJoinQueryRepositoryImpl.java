package com.elice.cinema.domain.movie.repository;

import com.elice.cinema.domain.movie.dto.internal.AdminMovieJoinRow;
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
    public List<AdminMovieJoinRow> findAdminMovieJoinRows(List<Long> movieIds) {

        EnumPath<Genre> genre = Expressions.enumPath(Genre.class, "genre");

        return queryFactory
                .select(Projections.constructor(
                        AdminMovieJoinRow.class,
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
                .where(movie.id.in(movieIds))
                .fetch();
    }
}
