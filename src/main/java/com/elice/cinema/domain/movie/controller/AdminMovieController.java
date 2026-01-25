package com.elice.cinema.domain.movie.controller;

import com.elice.cinema.domain.movie.dto.res.MovieResponse;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import com.elice.cinema.domain.movie.service.MovieReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/movies")
public class AdminMovieController {

    private final MovieReadService movieReadService;

    // 관리자 영화 목록 조회(검색 조건 + 페이징)
    @GetMapping
    public String getMovies(
            @RequestParam(required = false) MovieStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<MovieResponse> moviesPage;

        if (keyword != null && !keyword.isBlank()) {
            moviesPage = movieReadService.searchMovies(keyword, pageable);
        } else if (status != null) {
            moviesPage = movieReadService.getMovies(status, pageable);
        } else {
            moviesPage = movieReadService.getMovies(pageable);
        }

        model.addAttribute("movies", moviesPage.getContent());
        model.addAttribute("page", moviesPage);

        return "admin/movie/movie-list";
    }

    // 관리자 영화 상세 조회
    @GetMapping("/{movieId}")
    public String getMovieDetail(
            @PathVariable Long movieId,
            Model model
    ) {
        MovieResponse movie = movieReadService.getMovie(movieId);
        model.addAttribute("movie", movie);
        return "admin/movie/movie-detail";
    }


}
