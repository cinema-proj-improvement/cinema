package com.elice.cinema.domain.screening.controller;

import com.elice.cinema.domain.screening.service.ScreeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/api/screenings")
public class AdminScreeningSeatApiController {

    private final ScreeningService screeningService;

    // 상영별 좌석 현황 조회 (요약 + 목록)
    @GetMapping("/{screeningId}/seats")
    public Map<String, Object> getScreeningSeats(@PathVariable Long screeningId) {

        return Map.of(
                "summary", screeningService.getSeatSummary(screeningId),
                "seats", screeningService.getSeats(screeningId)
        );
    }
}
