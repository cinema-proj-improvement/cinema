package com.elice.cinema.global.home.controller;

import com.elice.cinema.global.home.dto.response.HomeMovieResponse;
import com.elice.cinema.global.home.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/")
    public String home(Model model) {

        // 예매율 TOP 4
        List<HomeMovieResponse> topMovies =
                homeService.getTop4Movies();

        model.addAttribute("topMovies", topMovies);

        // Hero는 1위 영화 (있을 때만)
        if (!topMovies.isEmpty()) {
            model.addAttribute("heroMovie", topMovies.get(0));
        }

        return "home";
    }
}
