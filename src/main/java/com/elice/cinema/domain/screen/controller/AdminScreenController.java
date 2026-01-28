package com.elice.cinema.domain.screen.controller;


import com.elice.cinema.domain.common.ScreeningType;
import com.elice.cinema.domain.screen.dto.request.ScreenCreateRequest;
import com.elice.cinema.domain.screen.dto.response.ScreenDetailResponse;
import com.elice.cinema.domain.screen.dto.response.ScreenListResponse;
import com.elice.cinema.domain.screen.service.ScreenService;
import com.elice.cinema.global.error.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/screens")
public class AdminScreenController {
    private final ScreenService screenService;

    @GetMapping
    public String getScreens(@RequestParam(required = false) Boolean operating,
                             Pageable pageable,
                             Model model) {
        Page<ScreenListResponse> screens = screenService.getScreens(operating, pageable);

        model.addAttribute("operating", operating);
        model.addAttribute("screens", screens);

        return "admin/screen/screen-list";
    }

    @GetMapping("/{screenId}")
    public String getScreenDetail(@PathVariable Long screenId, Model model) {
        ScreenDetailResponse screen = screenService.getScreenDetail(screenId);
        int availableSeats = screenService.getAvailableSeatCount(screenId);

        model.addAttribute("screen", screen);
        model.addAttribute("availableSeats", availableSeats);

        return "admin/screen/screen-detail";
    }

    @GetMapping("/new")
    public String showCreateScreenForm(Model model) {
        model.addAttribute("form", new ScreenCreateRequest());
        model.addAttribute("screeningTypes", ScreeningType.values());
        return "admin/screen/screen-create";
    }

    @PostMapping("/new")
    public String createScreen(
            @Valid @ModelAttribute("form") ScreenCreateRequest form,
            BindingResult bindingResult,
            Model model
    ) {
        // DTO 유효성 실패
        if (bindingResult.hasErrors()) {
            model.addAttribute("screeningTypes", ScreeningType.values());
            return "admin/screen/screen-create";
        }

        // 비즈니스 검증 (서비스 레벨)
        try {
            screenService.createScreen(form);
        } catch (BusinessException e) {
            bindingResult.reject("screen.create.fail", e.getMessage());
            model.addAttribute("screeningTypes", ScreeningType.values());
            return "admin/screen/screen-create";
        }

        // 성공 → 목록으로
        return "redirect:/admin/screens";
    }
}
