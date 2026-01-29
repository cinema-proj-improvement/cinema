package com.elice.cinema.domain.screening.controller;

import com.elice.cinema.domain.screening.dto.request.AdminScreeningSearchRequest;
import com.elice.cinema.domain.screening.dto.response.AdminScreeningResponse;
import com.elice.cinema.domain.screening.service.ScreeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/screenings")
public class AdminScreeningController {

    private final ScreeningService screeningService;

    @GetMapping
    public String getAdminScreenings(
            AdminScreeningSearchRequest request,
            @PageableDefault(size = 20) Pageable pageable,
            Model model
    ) {
        Page<AdminScreeningResponse> screenings =
                screeningService.searchAdmin(request, pageable);

        model.addAttribute("screenings", screenings);
        model.addAttribute("search", request);

        return "admin/screening/screening-list";
    }

}
