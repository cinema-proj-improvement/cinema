package com.elice.cinema.domain.mypage.controller;

import com.elice.cinema.domain.mypage.dto.MypageHomeResponse;
import com.elice.cinema.domain.mypage.service.MypageService;
import com.elice.cinema.domain.reservation.dto.response.MypageDetailReservationResponse;
import com.elice.cinema.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MypageController {
    private final MypageService mypageService;

    @GetMapping
    public String getMypageHome(Model model,
                                @AuthenticationPrincipal CustomUserDetails userDetail) {
        MypageHomeResponse response = mypageService.getMypageHome(userDetail.getMemberId());
        model.addAttribute("response", response);
        return "user/mypage/mypage-home";
    }

    @GetMapping("/reservations")
    public String getMypageReservations(Model model,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                        Pageable pageable,
                                        @AuthenticationPrincipal CustomUserDetails userDetail) {

        Slice<MypageDetailReservationResponse> reservations = mypageService.getMyReservations(userDetail.getMemberId(), from, to, pageable);
        model.addAttribute("reservations", reservations);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "user/mypage/mypage-reservations";
    }

    @PostMapping("/reservations/{reservationId}/cancel")
    public String cancelReservation(@PathVariable Long reservationId) {
        mypageService.cancelReservation(reservationId);
        return "redirect:/mypage/reservations";
    }
}
