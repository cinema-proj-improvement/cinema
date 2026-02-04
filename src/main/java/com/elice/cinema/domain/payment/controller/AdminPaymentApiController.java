package com.elice.cinema.domain.payment.controller;

import com.elice.cinema.domain.payment.service.AdminPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/api/payments")
public class AdminPaymentApiController {

    private final AdminPaymentService AdminpaymentService;

    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long paymentId) {
        AdminpaymentService.cancelByAdmin(paymentId);
        return ResponseEntity.ok().build();
    }
}
