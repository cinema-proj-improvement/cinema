package com.elice.cinema.domain.admin.controller;

import com.elice.cinema.global.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminHomeController {
    @GetMapping
    public String adminHome(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("cinemaName", "여운");  // 여운 극장
        return "admin/home";
    }
}
