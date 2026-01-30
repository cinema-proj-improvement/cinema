package com.elice.cinema.domain.screening.controller;

import com.elice.cinema.domain.common.ScreeningType;
import com.elice.cinema.domain.movie.dto.response.MovieSelectResponse;
import com.elice.cinema.domain.movie.service.MovieService;
import com.elice.cinema.domain.policy.service.EnvironmentPolicyService;
import com.elice.cinema.domain.screening.dto.reponse.ScreeningDetailResponse;
import com.elice.cinema.domain.screening.dto.reponse.ScreeningMovieOptionResponse;
import com.elice.cinema.domain.screening.dto.reponse.ScreeningTimetableResponse;
import com.elice.cinema.domain.screening.dto.request.ScreeningCreateRequest;
import com.elice.cinema.domain.screening.dto.request.ScreeningUpdateRequest;
import com.elice.cinema.domain.screening.entity.ScreeningStatus;
import com.elice.cinema.domain.screening.service.ScreeningOptionService;
import com.elice.cinema.domain.screening.service.ScreeningService;
import com.elice.cinema.global.error.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/{screeningId}")
    public String getScreeningDetail(@PathVariable Long screeningId,
                                     Model model) {
        ScreeningDetailResponse screening = screeningService.getScreeningDetail(screeningId);
        model.addAttribute("screening", screening);
        model.addAttribute("statuses", ScreeningStatus.values()); // 드랍다운 옵션

        ScreeningUpdateRequest form = new ScreeningUpdateRequest();
        form.setScreeningStatus(screening.getScreeningStatus()); // 현재값 세팅
        model.addAttribute("form", form);
        return "admin/screening/screening-detail";
    }

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

    //TODO: 상세 조회 만들고 나면 다시 만들기
    @PatchMapping("/{screeningId}/status")
    public String updateScreeningStatus(@PathVariable Long screeningId,
                                        @Valid @ModelAttribute("form") ScreeningUpdateRequest form,
                                        BindingResult bindingResult,
                                        Model model) {
        if (bindingResult.hasErrors()) {
            ScreeningDetailResponse screening = screeningService.getScreeningDetail(screeningId);
            model.addAttribute("screening", screening);
            model.addAttribute("statuses", ScreeningStatus.values());
            return "admin/screening/screening-detail";
        }

        // 2) 비즈니스 예외(상태 변경 불가 등) -> 상단 알림으로 노출
        try {
            screeningService.updateScreening(screeningId, form);
        } catch (BusinessException e) {
            ScreeningDetailResponse screening = screeningService.getScreeningDetail(screeningId);
            model.addAttribute("screening", screening);
            model.addAttribute("statuses", ScreeningStatus.values());

            model.addAttribute("errorMessage", e.getMessage());
            return "admin/screening/screening-detail";
        }

        return "redirect:/admin/screenings/{screeningId}";
    }

}
