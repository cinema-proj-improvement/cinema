package com.elice.cinema.domain.screen.controller;

import com.elice.cinema.domain.screen.dto.request.SeatUpdateRequest;
import com.elice.cinema.domain.screen.dto.response.SeatDetailResponse;
import com.elice.cinema.domain.screen.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/seats")
@RequiredArgsConstructor
public class AdminSeatController {

    private final SeatService seatService;

    @GetMapping("/{seatId}")
    public ResponseEntity<SeatDetailResponse> getSeatDetail(@PathVariable Long seatId) {
        return ResponseEntity.ok(seatService.getSeatDetail(seatId));
    }

    @PatchMapping("/{seatId}/active")
    public SeatDetailResponse updateSeatActive(@PathVariable Long seatId,
                                               @RequestBody SeatUpdateRequest request) {
        return seatService.updateSeatActive(seatId, request.getActive());
    }
}
