package com.elice.cinema.domain.movie.controller;

import com.elice.cinema.domain.movie.dto.request.AdminMovieSearchRequest;
import com.elice.cinema.domain.movie.dto.request.AdminMovieSortType;
import com.elice.cinema.domain.movie.dto.request.MovieCreateRequest;
import com.elice.cinema.domain.movie.dto.request.MovieUpdateRequest;
import com.elice.cinema.domain.movie.dto.response.MovieDetailResponse;
import com.elice.cinema.domain.movie.dto.response.MovieUpdateFormResponse;
import com.elice.cinema.domain.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/admin/movies")
@RequiredArgsConstructor
public class AdminMovieController {
    private final MovieService movieService;

    @GetMapping("/new")
    public String showCreateMovieForm(Model model) {
        model.addAttribute("form", new MovieCreateRequest());
        return "admin/movie/movie-create";
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String createMovie(@Validated @ModelAttribute("form") MovieCreateRequest req,
                              BindingResult bindingResult,
                              Model model) {
        if(bindingResult.hasErrors()) {
            return "admin/movie/movie-create";
        }

        Long movieId = movieService.createMovie(req);
        return "redirect:/admin/movies/" + movieId;
    }

    // 관리자 영화 목록 조회(검색 조건 + 페이징)
    @GetMapping
    public String getAdminMovieListPage(
            AdminMovieSearchRequest request,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            Model model

    ) {
        if (request.getSortType() == null) {
            request.setSortType(AdminMovieSortType.RELEASE_DATE_DESC);
        }

        model.addAttribute("moviesPage",
                movieService.getAdminMovieListPage(request, pageable));

        model.addAttribute("search", request);
        model.addAttribute("selectedSortType", request.getSortType().name());

        return "admin/movie/movie-list";
    }

    // 관리자 영화 상세 조회
    @GetMapping("/{movieId}")
    public String getAdminMovieDetail(
            @PathVariable Long movieId,
            Model model
    ) {
        MovieDetailResponse movie = movieService.getAdminMovieDetail(movieId);
        model.addAttribute("movie", movie);
        return "admin/movie/movie-detail";
    }

    @GetMapping("/{movieId}/edit")
    public String showUpdateMovieForm(
            @PathVariable Long movieId,
            Model model
    ){
        MovieUpdateFormResponse movie = movieService.getMovieUpdateForm(movieId);

        log.info("releaseDate = {}", movie.getReleaseDate());
        log.info("endDate = {}", movie.getEndDate());

        model.addAttribute("movieId", movieId);
        model.addAttribute("movie", movie);
        model.addAttribute("extraImages", movie.getExtraImages());
        return "admin/movie/movie-update";
    }

    @PostMapping("/{movieId}")
    public String updateMovie(@PathVariable Long movieId,
                              @Validated @ModelAttribute MovieUpdateRequest req) {
        movieService.updateMovie(movieId, req);

        return "redirect:/admin/movies/" + movieId;
    }
}
