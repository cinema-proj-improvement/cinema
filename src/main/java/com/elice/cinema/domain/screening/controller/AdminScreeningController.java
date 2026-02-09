package com.elice.cinema.domain.screening.controller;

import com.elice.cinema.domain.movie.dto.response.MovieSelectResponse;
import com.elice.cinema.domain.movie.service.MovieService;
import com.elice.cinema.domain.policy.service.EnvironmentPolicyService;
import com.elice.cinema.domain.reservation.dto.response.AdminReservationPageResponse;
import com.elice.cinema.domain.reservation.dto.response.AdminReservationSummaryResponse;
import com.elice.cinema.domain.reservation.entity.ReservationStatus;
import com.elice.cinema.domain.reservation.service.AdminReservationService;
import com.elice.cinema.domain.screening.dto.request.AdminScreeningSearchRequest;
import com.elice.cinema.domain.screening.dto.request.ScreeningCreateRequest;
import com.elice.cinema.domain.screening.dto.request.ScreeningUpdateRequest;
import com.elice.cinema.domain.screening.dto.response.AdminScreeningFilterOptionResponse;
import com.elice.cinema.domain.screening.dto.response.AdminScreeningResponse;
import com.elice.cinema.domain.screening.dto.response.ScreeningDetailResponse;
import com.elice.cinema.domain.screening.entity.ScreeningStatus;
import com.elice.cinema.domain.screening.service.ScreeningOptionService;
import com.elice.cinema.domain.screening.service.ScreeningService;
import com.elice.cinema.global.error.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/screenings")
@Slf4j
public class AdminScreeningController {
    private final ScreeningService screeningService;
    private final MovieService movieService;
    private final EnvironmentPolicyService environmentPolicyService;
    private final ScreeningOptionService screeningOptionService;
    private final AdminReservationService adminReservationService;

    @GetMapping("/{screeningId}")
    public String getScreeningDetail(@PathVariable Long screeningId,
                                     @RequestParam(required = false) ReservationStatus status,
                                     @PageableDefault(size = 20) Pageable pageable,
                                     Model model) {
        ScreeningDetailResponse screening = screeningService.getScreeningDetail(screeningId);
        model.addAttribute("screening", screening);
        model.addAttribute("statuses", ScreeningStatus.values()); // 드랍다운 옵션

        ScreeningUpdateRequest form = new ScreeningUpdateRequest();
        form.setScreeningStatus(screening.getScreeningStatus()); // 현재값 세팅
        model.addAttribute("form", form);

        Page<AdminReservationPageResponse> reservationsPage =
                adminReservationService.getAdminReservationListByScreening(
                        screeningId,
                        status,
                        pageable
                );

        AdminReservationSummaryResponse reservationSummary =
                adminReservationService.getReservationSummaryByScreening(screeningId);

        model.addAttribute("reservationsPage", reservationsPage);
        model.addAttribute("reservationSummary", reservationSummary);
        model.addAttribute("selectedStatus", status);
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

    @PatchMapping("/{screeningId}/status")
    public String updateScreeningStatus(@PathVariable Long screeningId,
                                        @Valid @ModelAttribute("form") ScreeningUpdateRequest form,
                                        BindingResult bindingResult,
                                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "상태 변경 요청 값이 올바르지 않습니다."
            );
            return "redirect:/admin/screenings/" + screeningId;
        }

        // 2) 비즈니스 예외 (상태 변경 불가 등)
        try {
            screeningService.updateScreening(screeningId, form);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "상영 상태가 변경되었습니다."
            );
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage()
            );
        }

        return "redirect:/admin/screenings/" + screeningId;
    }

    @GetMapping({"", "/"})
    public String getAdminScreenings(
            AdminScreeningSearchRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startAt,desc") String sort,
            Model model
    ) {
        // Pageable 객체 직접 생성
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(page, size);
        
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

    @DeleteMapping("/{screeningId}")
    public String deleteScreening(
            @PathVariable Long screeningId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            screeningService.deleteScreening(screeningId);
            return "redirect:/admin/screenings";

        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage()
            );
            return "redirect:/admin/screenings/" + screeningId;
        }
    }
}
