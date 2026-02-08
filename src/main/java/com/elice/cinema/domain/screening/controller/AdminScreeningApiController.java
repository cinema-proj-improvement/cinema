package com.elice.cinema.domain.screening.controller;

import com.elice.cinema.domain.common.ScreeningType;
import com.elice.cinema.domain.screening.dto.response.ScreeningMovieOptionResponse;
import com.elice.cinema.domain.screening.dto.response.ScreeningTimetableResponse;
import com.elice.cinema.domain.screening.service.ScreeningOptionService;
import com.elice.cinema.domain.screening.service.ScreeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/screenings")
public class AdminScreeningApiController {
    private final ScreeningOptionService screeningOptionService;
    private final ScreeningService screeningService;

    /**
     * 1) 영화 선택 → 해당 영화가 지원하는 상영 타입만 반환
     */
    @GetMapping("/options/types")
    @ResponseBody
    public ScreeningMovieOptionResponse getScreeningTypes(@RequestParam Long movieId) {
        return screeningOptionService.getScreeningTypesByMovie(movieId);
    }

    /**
     * 2) (영화 + 상영 타입) 선택 → 상영 타입을 지원하는 상영관 목록 반환
     */
    @GetMapping("/options/screens")
    @ResponseBody
    public ScreeningMovieOptionResponse getScreens(
            @RequestParam Long movieId,
            @RequestParam ScreeningType screeningType
    ) {
        return screeningOptionService.getScreensByMovieAndType(movieId, screeningType);
    }

    /**
     * 3) (상영관 + 날짜) 선택 → 해당 날짜의 상영 시간표 반환 (시간순)
     * GET /admin/screenings/timetable?screenId=1&date=2026-01-29
     */
    @GetMapping("/timetable")
    @ResponseBody
    public List<ScreeningTimetableResponse> getTimetable(
            @RequestParam Long screenId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return screeningService.getTimetable(screenId, date);
    }
}
