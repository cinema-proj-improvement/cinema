package com.elice.cinema.domain.movie.controller;

import com.elice.cinema.domain.movie.service.MovieService;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
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
    ) { // TODO: 예매율순 정렬은 예매좌석 구현 후 추가
        if ("reservationRate".equals(sort)) {
            throw new BusinessException(ErrorCode.MOVIE_SORT_NOT_SUPPORTED);
        }
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
