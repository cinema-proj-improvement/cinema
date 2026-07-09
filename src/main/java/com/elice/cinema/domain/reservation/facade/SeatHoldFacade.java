package com.elice.cinema.domain.reservation.facade;

import com.elice.cinema.domain.reservation.repository.ReservationLockRepository;
import com.elice.cinema.domain.reservation.service.ReservationService;
import com.elice.cinema.global.config.properties.SeatHoldProperties;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatHoldFacade {

    private final ReservationLockRepository reservationLockRepository;
    private final ReservationService reservationService;
    private final SeatHoldProperties seatHoldProperties;

    public Long holdSeats(Long screeningId, List<Long> seatIds, Long memberId) {
        reservationService.validateSeatCount(seatIds);
        reservationService.validateBookable(screeningId, seatIds);

        boolean acquired = reservationLockRepository.lockAll(
                screeningId, seatIds, memberId,
                seatHoldProperties.getLockTtlSeconds(), TimeUnit.SECONDS
        );
        if (!acquired) {
            log.warn("좌석 선점 실패 - 이미 선점된 좌석: screeningId={}, seatIds={}, memberId={}", screeningId, seatIds, memberId);
            throw new BusinessException(ErrorCode.SEAT_ALREADY_HELD);
        }

        try {
            Long reservationId = reservationService.createHoldReservation(screeningId, seatIds, memberId);
            reservationLockRepository.unlockAllSafely(screeningId, seatIds, memberId); // DB 커밋 성공 후 해제
            return reservationId;
        } catch (Exception e) {
            reservationLockRepository.unlockAllSafely(screeningId, seatIds, memberId); // 실패 시 즉시 해제
            throw e;
        }
    }
}
