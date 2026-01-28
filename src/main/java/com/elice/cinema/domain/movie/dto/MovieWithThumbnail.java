package com.elice.cinema.domain.movie.dto;

import com.elice.cinema.domain.movie.entity.Movie;

public record MovieWithThumbnail(Movie movie,
                                 String thumbnail) {
}
