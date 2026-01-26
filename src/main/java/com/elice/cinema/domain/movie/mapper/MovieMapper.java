package com.elice.cinema.domain.movie.mapper;

import com.elice.cinema.domain.movie.dto.request.MovieCreateRequest;
import com.elice.cinema.domain.movie.dto.response.MovieUpdateFormResponse;
import com.elice.cinema.domain.movie.dto.response.MovieResponse;
import com.elice.cinema.domain.movie.entity.Movie;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class MovieMapper {
    // DTO -> Entity 로직은 수동 작성합니다. 메서드명은 toEntity로 고정합니다. (param에 들어갈 타입이 다르기 때문에 의미 구분이 가능합니다.)
    public Movie toEntity(MovieCreateRequest req) {
        return Movie.of(
                req.getTitle(),
                req.getRunningTimeMinutes(),
                req.getReleaseDate(),
                req.getEndDate(),
                req.getAgeRating(),
                req.getSynopsis(),
                req.getThumbnailImageUrl()
        );
    }

    // TODO: (Entity -> DTO) 로직은 Mapstruct를 사용합니다. 해당 로직은 abstract 메서드로 정의해야 합니다.

    public abstract MovieResponse toResponse(Movie movie);
    public abstract MovieUpdateFormResponse toMovieUpdateFormResponse(Movie movie);
}
