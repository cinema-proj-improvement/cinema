package com.elice.cinema.domain.mypage.controller;

import com.elice.cinema.domain.mypage.dto.MypageHomeResponse;
import com.elice.cinema.domain.mypage.service.MypageService;
import com.elice.cinema.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MypageController {
    private final MypageService mypageService;

    @GetMapping
    public String getMypageHome(Model model,
                                @AuthenticationPrincipal CustomUserDetails userDetail) {
        MypageHomeResponse response = mypageService.getMypageHome(userDetail.getMemberId());
        model.addAttribute("response", response);
        return "user/mypage/mypage-home";
    }
}
