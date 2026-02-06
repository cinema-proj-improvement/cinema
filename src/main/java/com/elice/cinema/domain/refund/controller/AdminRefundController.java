package com.elice.cinema.domain.refund.controller;

import com.elice.cinema.domain.refund.dto.response.AdminRefundListResponse;
import com.elice.cinema.domain.refund.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/refunds")
public class AdminRefundController {
    private final RefundService refundService;

    @GetMapping
    public String refunds(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @RequestParam(required = false) String keyword,
            Pageable pageable,
            Model model
    ) {
        Page<AdminRefundListResponse> page = refundService.searchAdminRefunds(from, to, keyword, pageable);

        model.addAttribute("page", page);

        // 검색 값 유지용
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("keyword", keyword);

        return "admin/refund/refund-list";
    }
}
