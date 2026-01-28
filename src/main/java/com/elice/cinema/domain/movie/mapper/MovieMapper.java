package com.elice.cinema.domain.movie.mapper;

import com.elice.cinema.domain.movie.dto.MovieWithThumbnail;
import com.elice.cinema.domain.movie.dto.request.MovieCreateRequest;
import com.elice.cinema.domain.movie.dto.response.AdminMovieListResponse;
import com.elice.cinema.domain.movie.dto.response.MovieDetailResponse;
import com.elice.cinema.domain.movie.dto.response.MovieListResponse;
import com.elice.cinema.domain.movie.dto.response.MovieUpdateFormResponse;
import com.elice.cinema.domain.movie.entity.Movie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

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

    // TODO: (Entity -> DTO) 로직은 Mapstruct를 사용합니다. 해당 로직은 abstract 메서드로 정의해야 합니다.

    AdminMovieListResponse toAdminListResponse(Movie movie, String thumbnail);

    MovieUpdateFormResponse toMovieUpdateFormResponse(Movie movie);
    MovieDetailResponse toMovieDetailResponse(
            Movie movie,
            String thumbnail,
            List<String> images
    );

    @Mapping(source = "movie.id", target = "id")
    @Mapping(source = "movie.title", target = "title")
    @Mapping(source = "movie.releaseDate", target = "releaseDate")
    @Mapping(source = "movie.avgScore", target = "avgScore")
    @Mapping(source = "movie.advanceReservationRate", target = "advanceReservationRate")
    @Mapping(source = "thumbnail", target = "thumbnail")
    MovieListResponse toMovieListResponse(MovieWithThumbnail source);
}
