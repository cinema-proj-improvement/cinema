package com.elice.cinema.domain.screening.dto.response;

import com.elice.cinema.domain.common.ScreeningType;
import com.elice.cinema.domain.movie.dto.response.MovieTitleResponse;
import com.elice.cinema.domain.screen.dto.response.ScreenNameResponse;
import com.elice.cinema.domain.screening.entity.ScreeningStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ScreeningDetailResponse {
    private Long id;
    private MovieTitleResponse movie;
    private ScreenNameResponse screen;
    private ScreeningType screeningType;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private ScreeningStatus screeningStatus;
}
