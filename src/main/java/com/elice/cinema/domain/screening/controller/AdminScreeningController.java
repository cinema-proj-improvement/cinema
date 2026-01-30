package com.elice.cinema.domain.screening.controller;

import com.elice.cinema.domain.common.ScreeningType;
import com.elice.cinema.domain.movie.dto.response.MovieSelectResponse;
import com.elice.cinema.domain.movie.service.MovieService;
import com.elice.cinema.domain.policy.service.EnvironmentPolicyService;
import com.elice.cinema.domain.screening.dto.request.AdminScreeningSearchRequest;
import com.elice.cinema.domain.screening.dto.request.ScreeningCreateRequest;
import com.elice.cinema.domain.screening.dto.response.AdminScreeningFilterOptionResponse;
import com.elice.cinema.domain.screening.dto.response.AdminScreeningResponse;
import com.elice.cinema.domain.screening.dto.response.ScreeningMovieOptionResponse;
import com.elice.cinema.domain.screening.dto.response.ScreeningTimetableResponse;
import com.elice.cinema.domain.screening.service.ScreeningOptionService;
import com.elice.cinema.domain.screening.service.ScreeningService;
import com.elice.cinema.global.error.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/screenings")
public class AdminScreeningController {

    private final ScreeningService screeningService;
    private final MovieService movieService;
    private final EnvironmentPolicyService environmentPolicyService;
    private final ScreeningOptionService screeningOptionService;

    @GetMapping("/new")
    public String showCreateScreeningForm(Model model) {
        List<MovieSelectResponse> movies = movieService.getAvailableMoviesForScreening();

        model.addAttribute("movies", movies);
        model.addAttribute("cleaningMinutes", environmentPolicyService.getCleaningMinutes());
        model.addAttribute("form", new ScreeningCreateRequest());

        model.addAttribute("screeningTypes", List.of());
        model.addAttribute("screens", List.of());
        return "admin/screening/screening-create";
    }

    /**
     * 1) 영화 선택 → 해당 영화가 지원하는 상영 타입만 반환
     */
    @GetMapping("/options/types")
    @ResponseBody
    public ScreeningMovieOptionResponse getScreeningTypes(@RequestParam Long movieId) {
        return screeningOptionService.getScreeningTypesByMovie(movieId);
    }

    /**
     * 2) (영화 + 상영 타입) 선택 → 상영 타입을 지원하는 상영관 목록 반환
     */
    @GetMapping("/options/screens")
    @ResponseBody
    public ScreeningMovieOptionResponse getScreens(
            @RequestParam Long movieId,
            @RequestParam ScreeningType screeningType
    ) {
        return screeningOptionService.getScreensByMovieAndType(movieId, screeningType);
    }
    /**
     * 3) (상영관 + 날짜) 선택 → 해당 날짜의 상영 시간표 반환 (시간순)
     * GET /admin/screenings/timetable?screenId=1&date=2026-01-29
     */
    @GetMapping("/timetable")
    @ResponseBody
    public List<ScreeningTimetableResponse> getTimetable(
            @RequestParam Long screenId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return screeningService.getTimetable(screenId, date);
    }

    @PostMapping("/new")
    public String createScreening(@Valid @ModelAttribute("form") ScreeningCreateRequest form,
                                  BindingResult bindingResult,
                                  Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("movies", movieService.getAvailableMoviesForScreening());
            model.addAttribute("cleaningMinutes", environmentPolicyService.getCleaningMinutes());
            model.addAttribute("screens", List.of());
            return "admin/screening/screening-create";
        }

        try {
            screeningService.createScreening(form);
        } catch (BusinessException e) {
            bindingResult.reject("screening.create.fail", e.getMessage());

            model.addAttribute("movies", movieService.getAvailableMoviesForScreening());
            model.addAttribute("cleaningMinutes", environmentPolicyService.getCleaningMinutes());
            model.addAttribute("screens", List.of());
            return "admin/screening/screening-create";
        }

        return "redirect:/admin/screenings";
    }

    @GetMapping({"", "/"})
    public String getAdminScreenings(
            AdminScreeningSearchRequest request,
            @PageableDefault(size = 20) Pageable pageable,
            Model model
    ) {
        Page<AdminScreeningResponse> screenings =
                screeningService.searchAdmin(request, pageable);

        List<AdminScreeningFilterOptionResponse> movieFilterOptions =
                screeningService.getMovieFilterOptions();

        List<AdminScreeningFilterOptionResponse> screenFilterOptions =
                screeningService.getScreenFilterOptions();

        model.addAttribute("screenings", screenings);
        model.addAttribute("search", request);
        model.addAttribute("movieFilterOptions", movieFilterOptions);
        model.addAttribute("screenFilterOptions", screenFilterOptions);



        return "admin/screening/screening-list";
    }

}
