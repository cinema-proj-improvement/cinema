package com.elice.cinema.domain.auth.controller;

import com.elice.cinema.domain.auth.dto.request.SignupRequest;
import com.elice.cinema.domain.member.service.MemberService;
import com.elice.cinema.global.error.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
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
            log.warn("회원가입 유효성 검사 실패: errors={}", bindingResult.getAllErrors());
            return "auth/signup";
        }

        try {
            memberService.signup(request);
        } catch (BusinessException e) {
            log.warn("회원가입 실패 - {}: email={}", e.getErrorCode().getMessage(), request.getEmail());
            model.addAttribute("signupError", e.getErrorCode().getMessage());
            return "auth/signup";
        }

        return "redirect:/login";
    }
}
