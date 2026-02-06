package com.elice.cinema.domain.policy.controller;

import com.elice.cinema.domain.policy.service.RefundPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/policies/refunds")
public class AdminRefundPolicyController {

    private final RefundPolicyService refundPolicyService;

    @GetMapping
    public String refundPolicyPage(Model model) {
        model.addAttribute("policies", refundPolicyService.findAll());
        return "admin/policy/refund-policy";
    }
}