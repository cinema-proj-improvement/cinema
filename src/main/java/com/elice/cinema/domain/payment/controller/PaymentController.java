package com.elice.cinema.domain.payment.controller;

import com.elice.cinema.domain.payment.service.PaymentService;
import com.elice.cinema.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping("/success")
    public String paymentSuccess(@RequestParam String paymentKey,
                                 @RequestParam String orderId,
                                 @RequestParam Long amount,
                                 @AuthenticationPrincipal CustomUserDetails userDetail) {
        paymentService.handleSuccess(paymentKey, orderId, amount, userDetail.getMemberId());
        return "user/payment/success";
    }

    //TODO: 추후 좌석 선택 화면으로 연결할지 고민
    @GetMapping("/fail")
    public String paymentFail(@RequestParam String message,
                              @RequestParam String code,
                              Model model) {
        model.addAttribute("code", code);
        model.addAttribute("message", message);
        return "user/payment/fail";
    }
}
