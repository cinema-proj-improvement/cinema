package com.elice.cinema.domain.payment.controller;

import com.elice.cinema.domain.payment.service.PaymentConfirmService;
import com.elice.cinema.domain.reservation.dto.response.ReservationIdResponse;
import com.elice.cinema.domain.reservation.service.ReservationService;
import com.elice.cinema.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentConfirmService paymentSuccessService;
    private final ReservationService reservationService;

    @GetMapping("/success")
    public String paymentSuccess(@RequestParam String paymentKey,
                                 @RequestParam String orderId,
                                 @RequestParam Long amount,
                                 @AuthenticationPrincipal CustomUserDetails userDetail) {
        paymentSuccessService.handleSuccess(paymentKey, orderId, amount, userDetail.getMemberId());
        return "user/payment/success";
    }

    @GetMapping("/fail")
    public String paymentFail(@RequestParam(required = false) String code,
                              @RequestParam String orderId,
                              RedirectAttributes ra) {
        ReservationIdResponse reservation = reservationService.getReservationIdByReservationCode(orderId);
        ra.addAttribute("error", true);
        ra.addAttribute("message", paymentSuccessService.resolveFailMessage(code));
        return "redirect:/reservations/" + reservation.getId();
    }
}
