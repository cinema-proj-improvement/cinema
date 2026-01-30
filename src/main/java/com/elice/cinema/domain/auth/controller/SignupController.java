package com.elice.cinema.domain.auth.controller;

import com.elice.cinema.domain.auth.dto.request.SignupRequest;
import com.elice.cinema.domain.member.service.MemberService;
import com.elice.cinema.global.error.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/signup")
public class SignupController {

    private final MemberService memberService;

    @GetMapping
    public String signupPage(Model model) {
        model.addAttribute("signupRequest", new SignupRequest());
        return "auth/signup";
    }

    @PostMapping
    public String signup(
            @Valid SignupRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }

        try {
            memberService.signup(request);
        } catch (BusinessException e) {
            model.addAttribute("signupError", e.getErrorCode().getMessage());
            return "auth/signup";
        }

        return "redirect:/login";
    }
}
