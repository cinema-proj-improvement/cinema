package com.elice.cinema.domain.movie.controller;

import com.elice.cinema.domain.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/movies")
public class MovieController {

    private final MovieService movieService;

    @GetMapping
    public String movieList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            Pageable pageable,
            Model model
    ) {
        model.addAttribute("moviesPage", movieService.getUserMovieList(keyword, sort, pageable));
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);

        return "user/movie/movie-list";
    }

    @GetMapping("/{movieId}")
    public String movieDetail(
            @PathVariable Long movieId,
            Model model
    ) {
        model.addAttribute("movie", movieService.getUserMovieDetail(movieId));

        return "user/movie/movie-detail";
    }


}
