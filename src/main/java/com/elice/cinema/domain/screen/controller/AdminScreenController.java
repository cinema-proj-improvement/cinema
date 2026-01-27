package com.elice.cinema.domain.screen.controller;


import com.elice.cinema.domain.screen.dto.response.ScreenDetailResponse;
import com.elice.cinema.domain.screen.dto.response.ScreenListResponse;
import com.elice.cinema.domain.screen.service.ScreenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
}
