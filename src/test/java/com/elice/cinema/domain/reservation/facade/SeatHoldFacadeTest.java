package com.elice.cinema.domain.reservation.facade;

import com.elice.cinema.domain.reservation.repository.ReservationLockRepository;
import com.elice.cinema.domain.reservation.service.ReservationService;
import com.elice.cinema.global.config.properties.SeatHoldProperties;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SeatHoldFacadeTest {

    @InjectMocks
    private SeatHoldFacade seatHoldFacade;

    @Mock private ReservationLockRepository reservationLockRepository;
    @Mock private ReservationService reservationService;
    @Mock private SeatHoldProperties seatHoldProperties;

    @Test
    void holdSeats_whenLockAllFails_thenThrow_andNeverCallCreateHoldReservation() {
        // given
        Long screeningId = 1L;
        Long memberId = 99L;
        List<Long> seatIds = List.of(101L, 102L);

        given(seatHoldProperties.getLockTtlSeconds()).willReturn(15);
        given(reservationLockRepository.lockAll(eq(screeningId), eq(seatIds), eq(memberId), anyLong(), eq(TimeUnit.SECONDS)))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> seatHoldFacade.holdSeats(screeningId, seatIds, memberId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SEAT_ALREADY_HELD));

        then(reservationService).should(never()).createHoldReservation(any(), anyList(), any());
        then(reservationLockRepository).should(never()).unlockAllSafely(any(), anyList(), any());
    }

    @Test
    void holdSeats_whenDbWorkFails_thenUnlockAndRethrow() {
        // given
        Long screeningId = 1L;
        Long memberId = 99L;
        List<Long> seatIds = List.of(101L, 102L);

        given(seatHoldProperties.getLockTtlSeconds()).willReturn(15);
        given(reservationLockRepository.lockAll(eq(screeningId), eq(seatIds), eq(memberId), anyLong(), eq(TimeUnit.SECONDS)))
                .willReturn(true);
        willThrow(new DataIntegrityViolationException("conflict"))
                .given(reservationService).createHoldReservation(screeningId, seatIds, memberId);

        // when & then
        assertThatThrownBy(() -> seatHoldFacade.holdSeats(screeningId, seatIds, memberId))
                .isInstanceOf(DataIntegrityViolationException.class);

        then(reservationLockRepository).should().unlockAllSafely(screeningId, seatIds, memberId);
    }

    @Test
    void holdSeats_success_thenUnlockAfterCommit() {
        // given
        Long screeningId = 1L;
        Long memberId = 99L;
        List<Long> seatIds = List.of(101L, 102L);

        given(seatHoldProperties.getLockTtlSeconds()).willReturn(15);
        given(reservationLockRepository.lockAll(eq(screeningId), eq(seatIds), eq(memberId), anyLong(), eq(TimeUnit.SECONDS)))
                .willReturn(true);
        given(reservationService.createHoldReservation(screeningId, seatIds, memberId)).willReturn(777L);

        // when
        Long reservationId = seatHoldFacade.holdSeats(screeningId, seatIds, memberId);

        // then
        assertThat(reservationId).isEqualTo(777L);
        then(reservationLockRepository).should().unlockAllSafely(screeningId, seatIds, memberId);
    }
}
