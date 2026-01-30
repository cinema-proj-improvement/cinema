package com.elice.cinema.domain.screen.service;

import com.elice.cinema.domain.screen.dto.response.SeatDetailResponse;
import com.elice.cinema.domain.screen.entity.Seat;
import com.elice.cinema.domain.screen.mapper.SeatMapper;
import com.elice.cinema.domain.screen.repository.SeatRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatService {
    private final SeatRepository seatRepository;
    private final SeatMapper seatMapper;

    public SeatDetailResponse getSeatDetail(Long seatId) {
        Seat seat = findSeatById(seatId);

        return seatMapper.toSeatDetailResponse(seat);
    }

    @Transactional
    public SeatDetailResponse updateSeatActive(Long seatId, Boolean active) {
        Seat seat = findSeatById(seatId);

        //TODO: 상영 객체 만든 후 "해당 상영관과 연관된 상영이 존재하지 않을 때에만 수정 가능" 조건 추가 하기
        /*if (Boolean.FALSE.equals(active)) {
            boolean hasScreening = screeningRepository.existsByScreenIdAndStatusIn(
                    seat.getScreen().getId(),
                    List.of(ScreeningStatus.OPEN, ScreeningStatus.SCHEDULED)
            );
            if (hasScreening) {
                throw new IllegalStateException("상영이 존재하는 상영관은 좌석 비활성화가 불가능합니다.");
            }
        }*/

        seat.setActive(active);

        return seatMapper.toSeatDetailResponse(seat);
    }

    // === Helper Methods ===
    private Seat findSeatById(Long seatId) {
        return seatRepository.findById(seatId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));
    }
}
