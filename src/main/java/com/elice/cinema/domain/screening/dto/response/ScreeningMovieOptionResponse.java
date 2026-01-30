package com.elice.cinema.domain.screening.dto.response;

import com.elice.cinema.domain.common.ScreeningType;
import com.elice.cinema.domain.screen.dto.response.ScreenSelectResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Set;

@Getter
@AllArgsConstructor
public class ScreeningMovieOptionResponse {
    private Set<ScreeningType> movieScreeningTypes; // 영화가 지원하는 타입들
    private List<ScreenSelectResponse> screens;     // 그 타입과 매칭되는 상영관들

    public static ScreeningMovieOptionResponse ofTypes(Set<ScreeningType> movieScreeningTypes) {
        return new ScreeningMovieOptionResponse(movieScreeningTypes, List.of());
    }

    public static ScreeningMovieOptionResponse ofScreens(List<ScreenSelectResponse> screens) {
        return new ScreeningMovieOptionResponse(Set.of(), screens);
    }
}
