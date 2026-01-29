package com.elice.cinema.domain.screening.service;

import com.elice.cinema.domain.screening.dto.reponse.ScreeningTimetableResponse;
import com.elice.cinema.domain.screening.entity.Screening;
import com.elice.cinema.domain.screening.mapper.ScreeningMapper;
import com.elice.cinema.domain.screening.repository.ScreeningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScreeningService {
    private final ScreeningRepository screeningRepository;
    private final ScreeningMapper screeningMapper;

    public List<ScreeningTimetableResponse> getTimetable(Long screenId, LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();

        List<Screening> screenings =
                screeningRepository.findTimetableByScreenAndDate(
                        screenId, from, to
                );

        return screenings.stream()
                .map(screeningMapper::toScreeningTimetableResponse)
                .toList();
    }
}
