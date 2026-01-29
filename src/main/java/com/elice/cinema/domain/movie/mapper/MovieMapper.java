package com.elice.cinema.domain.movie.mapper;

import com.elice.cinema.domain.movie.dto.request.MovieCreateRequest;
import com.elice.cinema.domain.movie.dto.response.AdminMovieListResponse;
import com.elice.cinema.domain.movie.dto.response.MovieUpdateFormResponse;
import com.elice.cinema.domain.movie.entity.Movie;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MovieMapper {
    default Movie toEntity(MovieCreateRequest req) {
        return Movie.createUpcomming(
                req.getTitle(),
                req.getRunningTimeMinutes(),
                req.getReleaseDate(),
                req.getEndDate(),
                req.getAgeRating(),
                req.getSynopsis(),
                req.getGenres(),
                req.getScreeningTypes()
        );
    }

    AdminMovieListResponse toAdminListResponse(Movie movie);
    MovieUpdateFormResponse toMovieUpdateFormResponse(Movie movie);
}
