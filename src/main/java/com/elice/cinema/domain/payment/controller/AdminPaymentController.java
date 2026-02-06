package com.elice.cinema.domain.payment.controller;

import com.elice.cinema.domain.payment.dto.request.AdminPaymentSearchCondition;
import com.elice.cinema.domain.payment.dto.response.AdminPaymentListResponse;
import com.elice.cinema.domain.payment.entity.PaymentStatus;
import com.elice.cinema.domain.payment.service.AdminPaymentService;
import com.elice.cinema.domain.payment.service.PaymentCancelService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/payments")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;
    private final PaymentCancelService paymentCancelService;

    // 관리자 결제 목록 조회
    @GetMapping
    public String AdminPaymentList(AdminPaymentSearchCondition condition, Pageable pageable, Model model) {
        Page<AdminPaymentListResponse> page =
                adminPaymentService.getAdminPaymentList(condition, pageable);

        model.addAttribute("page", page);
        model.addAttribute("condition", condition);
        model.addAttribute("paymentStatuses", PaymentStatus.values());
        return "admin/payment/payment-list";
    }

    // 관리자 결제 상세 조회
    @GetMapping("/{paymentId}")
    public String AdminPaymentDetail(@PathVariable Long paymentId, Model model) {

        model.addAttribute("payment", adminPaymentService.getAdminPaymentDetail(paymentId));
        return "admin/payment/payment-detail";
    }

    // 관리자 결제 취소
    @PostMapping("/{paymentId}/cancel")
    public String cancelPaymentByAdmin(
            @PathVariable Long paymentId
    ) {
        paymentCancelService.cancel(paymentId);
        return "redirect:/admin/payments";
    }
}
