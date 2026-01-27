package com.elice.cinema.domain.screen.service;

import com.elice.cinema.domain.screen.dto.response.ScreenListResponse;
import com.elice.cinema.domain.screen.mapper.ScreenMapper;
import com.elice.cinema.domain.screen.repository.ScreenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScreenService {
    private final ScreenRepository screenRepository;
    private final ScreenMapper screenMapper;

    public Page<ScreenListResponse> getScreens(Boolean operating, Pageable pageable) {
        if (operating == null) {
            return screenRepository.findAll(pageable)
                    .map(screenMapper::toScreenListResponse);
        }

        return screenRepository.findByOperating(operating, pageable)
                .map(screenMapper::toScreenListResponse);
    }
}
