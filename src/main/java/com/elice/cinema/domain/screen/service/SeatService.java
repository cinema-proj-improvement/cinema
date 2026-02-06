package com.elice.cinema.domain.screen.service;

import com.elice.cinema.domain.screen.dto.response.SeatDetailResponse;
import com.elice.cinema.domain.screen.entity.Seat;
import com.elice.cinema.domain.screen.mapper.SeatMapper;
import com.elice.cinema.domain.screen.repository.SeatRepository;
import com.elice.cinema.domain.screening.entity.ScreeningStatus;
import com.elice.cinema.domain.screening.repository.ScreeningRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatService {
    private final SeatRepository seatRepository;
    private final ScreeningRepository screeningRepository;
    private final SeatMapper seatMapper;

    public SeatDetailResponse getSeatDetail(Long seatId) {
        Seat seat = findSeatById(seatId);

        return seatMapper.toSeatDetailResponse(seat);
    }

    @Transactional
    public SeatDetailResponse updateSeatActive(Long seatId, Boolean active) {
        Seat seat = findSeatById(seatId);

        if (Boolean.FALSE.equals(active)) {
            validateNoActiveScreenings(seat.getScreen().getId());
        }

        seat.setActive(active);

        return seatMapper.toSeatDetailResponse(seat);
    }

    public List<Seat> getSeatsByScreenId(Long screenId) {
        return seatRepository.findAllByScreenId(screenId);
    }

    // === Helper Methods ===
    private Seat findSeatById(Long seatId) {
        return seatRepository.findById(seatId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));
    }

    private void validateNoActiveScreenings(Long screenId) {
        if (screeningRepository.existsByScreenIdAndScreeningStatusNot(screenId, ScreeningStatus.FINISHED)) {
            throw new BusinessException(ErrorCode.SEAT_UPDATE_NOT_ALLOWED_WHEN_SCREENING_NOT_FINISHED);
        }
    }
}
