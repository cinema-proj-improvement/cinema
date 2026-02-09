package com.elice.cinema.domain.movie.mapper;

import com.elice.cinema.domain.movie.dto.request.MovieCreateRequest;
import com.elice.cinema.domain.movie.dto.response.MovieDetailResponse;
import com.elice.cinema.domain.movie.dto.response.MovieListResponse;
import com.elice.cinema.domain.movie.dto.response.MovieUpdateFormResponse;
import com.elice.cinema.domain.movie.dto.response.*;
import com.elice.cinema.domain.movie.entity.Movie;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MovieMapper {
    default Movie toEntity(MovieCreateRequest req) {
        return Movie.createUpcoming(
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

    MovieUpdateFormResponse toMovieUpdateFormResponse(Movie movie);

    MovieDetailResponse toMovieDetailResponse(
            Movie movie,
            String thumbnail,
            List<String> images
    );

    default MovieListResponse toMovieListResponse(MovieListResponse response) {
        return response;
    }
    MovieSelectResponse toMovieSelectResponse(Movie movie);
    ReservationMovieSelectResponse toReservationMovieSelectResponse(Movie movie);
}
