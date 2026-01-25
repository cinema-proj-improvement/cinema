package com.elice.cinema.domain.movie.controller;

import com.elice.cinema.domain.movie.dto.request.MovieCreateRequest;
import com.elice.cinema.domain.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
        /*
            "영화" 버튼 클릭하면 이 메서드 호출되도록 연결 (생성 화면 html 코드 짜야 함)
            사용자가 해당 html 화면에서 폼 작성하여 영화 생성 request DTO 백엔드 서버로 전달
            해당 dto 받기 처리하는 컨트롤러 POST 메서드 작성 필요
            해당 dto 받아서 Movie entity 생성 후 DB에 저장하는 서비스 로직 작성해야 함
            POST 메서드에서 위 서비스 메서드 호출하여 처리
         */
    }

    @PostMapping
    public String createMovie(@Validated @ModelAttribute MovieCreateRequest req) {
        Long movieId = movieService.createMovie(req);
        return "redirect:/admin/movies/" + movieId;  // TODO: 상세 조회 메서드 주소와 매핑 필요
    }
}
