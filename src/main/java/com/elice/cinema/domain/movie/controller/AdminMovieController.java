package com.elice.cinema.domain.movie.controller;

import com.elice.cinema.domain.movie.dto.request.AdminMovieSearchRequest;
import com.elice.cinema.domain.movie.dto.response.AdminMovieListResponse;
import com.elice.cinema.domain.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/movies")
public class AdminMovieController {

    private final MovieService movieService;

    // 관리자 영화 목록 조회(검색 조건 + 페이징)
    @GetMapping
    public String getAdminMovieListPage(
            AdminMovieSearchRequest request,
            Pageable pageable,
            Model model

    ) {
        Page<AdminMovieListResponse> moviesPage = movieService.getAdminMovieListPage(request, pageable);

        model.addAttribute("moviesPage", moviesPage);
        model.addAttribute("search", request);

        return "admin/movie/movie-list";
    }

    // 관리자 영화 상세 조회
    @GetMapping("/{movieId}")
    public String getAdminMovieDetail(
            @PathVariable Long movieId,
            Model model
    ) {
        AdminMovieListResponse movie = movieService.getAdminMovieDetail(movieId);
        model.addAttribute("movie", movie);
        return "admin/movie/movie-detail";
    }
}
