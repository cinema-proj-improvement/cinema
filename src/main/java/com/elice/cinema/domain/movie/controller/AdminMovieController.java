package com.elice.cinema.domain.movie.controller;

import com.elice.cinema.domain.movie.dto.res.MovieResponse;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import com.elice.cinema.domain.movie.service.MovieReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/movies")
public class AdminMovieController {

    private final MovieReadService movieReadService;

    // 영화 목록 조회
    @GetMapping
    public String getMovies(
            @RequestParam(required = false) MovieStatus status,
            Model model
    ) {
        List<MovieResponse> movies =
                (status == null)
                        ? movieReadService.getMovies()
                        : movieReadService.getMovies(status);

        model.addAttribute("movies", movies);
        model.addAttribute("status", status);

        return "movie/movie-list";
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

    // 검색 조회
    @GetMapping
    public String getMovies(
            @RequestParam(required = false) MovieStatus status,
            @RequestParam(required = false) String keyword,
            Model model
    ) {
        List<MovieResponse> movies;

        if (keyword != null && !keyword.isBlank()) {
            movies = movieReadService.searchMovies(keyword);
        } else if (status != null) {
            movies = movieReadService.getMovies(status);
        } else {
            movies = movieReadService.getMovies();
        }

        model.addAttribute("movies", movies);
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);

        return "admin/movie/movie-list"; // html 추후
    }
}
