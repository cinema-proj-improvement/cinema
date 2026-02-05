package com.elice.cinema.domain.refund.controller;

import com.elice.cinema.domain.refund.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/refunds")
public class AdminRefundHistoryController {
    private final RefundService refundService;
}
