package com.elice.cinema.domain.movie.controller;

import com.elice.cinema.domain.movie.dto.request.MovieCreateRequest;
import com.elice.cinema.domain.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/movies")
@RequiredArgsConstructor
public class AdminMovieController {
    private final MovieService movieService;

    @GetMapping("/new")
    public String showCreateMovieForm(Model model) {
        model.addAttribute("form", new MovieCreateRequest());
        return "admin/movie/movie-create";
        // TODO: 관리자 영화 생성 화면 html 파일(movie-create.html) 작성 필요
    }

    // TODO: 폼 유효성 검사 실패 시 다시 폼으로 돌려보내는 기능 추가 필요? (BindingResult 사용?) (GPT 물어보기)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String createMovie(@Validated @ModelAttribute("form") MovieCreateRequest req,
                              BindingResult bindingResult,
                              Model model) {
        if(bindingResult.hasErrors()) {
            return "admin/movie/movie-create";
        }

        Long movieId = movieService.createMovie(req);
        return "redirect:/admin/movies/" + movieId;  // TODO: 상세 조회 메서드 주소와 매핑 필요
    }
}
