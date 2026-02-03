package com.elice.cinema.domain.payment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/payments")
public class PaymentController {
    @GetMapping("/success")
    public String paymentSuccess(@RequestParam String paymentKey,
                                 @RequestParam String orderId,
                                 @RequestParam Long amount,
                                 Model model) {
        // 👉 여기서 "결제 승인"을 서버에서 해야 함 (아직 안 함)
        model.addAttribute("orderId", orderId);
        return "user/payment/success";
    }

    @GetMapping("/fail")
    public String paymentFail(@RequestParam String message,
                              @RequestParam String code,
                              Model model) {
        model.addAttribute("code", code);
        model.addAttribute("message", message);
        return "user/payment/fail";
    }
}
