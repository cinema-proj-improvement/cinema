package com.elice.cinema.domain.screening.dto.response;

import com.elice.cinema.domain.common.ScreeningType;
import com.elice.cinema.domain.movie.dto.response.MovieTitleResponse;
import com.elice.cinema.domain.screen.dto.response.ScreenNameResponse;
import com.elice.cinema.domain.screening.entity.ScreeningStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class AdminScreeningResponse {

    private final Long id;

    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;

    private final MovieTitleResponse movie;
    private final ScreenNameResponse screen;
    private final ScreeningType screeningType;

    private final ScreeningStatus screeningStatus;

    // 좌석 현황
    private final AdminScreeningSeatSummaryResponse seatSummary;

    public static AdminScreeningResponse withSeatSummary(
            AdminScreeningResponse base,
            AdminScreeningSeatSummaryResponse seatSummary
    ) {
        return new AdminScreeningResponse(
                base.id,
                base.date,
                base.startTime,
                base.endTime,
                base.movie,
                base.screen,
                base.screeningType,
                base.screeningStatus,
                seatSummary
        );
    }
}
