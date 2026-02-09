package com.elice.cinema.domain.screen.service;

import com.elice.cinema.domain.screen.dto.response.SeatDetailResponse;
import com.elice.cinema.domain.screen.entity.Screen;
import com.elice.cinema.domain.screen.entity.Seat;
import com.elice.cinema.domain.screen.mapper.SeatMapper;
import com.elice.cinema.domain.screen.repository.SeatRepository;
import com.elice.cinema.domain.screening.entity.ScreeningStatus;
import com.elice.cinema.domain.screening.repository.ScreeningRepository;
import com.elice.cinema.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

    @Mock SeatRepository seatRepository;
    @Mock ScreeningRepository screeningRepository;
    @Mock SeatMapper seatMapper;

    @InjectMocks SeatService seatService;

    @Test
    @DisplayName("좌석 활성화 성공 - active=true는 상영 여부를 검사하지 않는다")
    void updateSeatActive_true_success() {
        // given
        Long seatId = 1L;
        Screen screen = Screen.of("1관", null, 10, true);
        Seat seat = Seat.of(screen, "A1", false, 1, 1);

        SeatDetailResponse dto = mock(SeatDetailResponse.class);

        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));
        when(seatMapper.toSeatDetailResponse(seat)).thenReturn(dto);

        // when
        SeatDetailResponse result = seatService.updateSeatActive(seatId, true);

        // then
        assertTrue(seat.isActive());
        assertSame(dto, result);

        verify(seatRepository).findById(seatId);
        verify(seatMapper).toSeatDetailResponse(seat);
        verifyNoInteractions(screeningRepository);
    }

    @Test
    @DisplayName("좌석 활성/비활성 실패 - 존재하지 않는 좌석 ID")
    void updateSeatActive_seatNotFound_fail() {
        // given
        Long seatId = 999L;
        when(seatRepository.findById(seatId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(BusinessException.class,
                () -> seatService.updateSeatActive(seatId, true));

        verify(seatRepository).findById(seatId);
        verifyNoInteractions(screeningRepository);
        verifyNoInteractions(seatMapper);
    }

    @Test
    @DisplayName("좌석 비활성화 실패 - 진행 중인 상영이 존재하는 상영관")
    void updateSeatActive_false_whenActiveScreeningsExist_fail() {
        // given
        Long seatId = 2L;
        Long screenId = 10L;

        Screen screen = Screen.of("1관", null, 10, true);
        Screen spyScreen = spy(screen);
        doReturn(screenId).when(spyScreen).getId();

        Seat seat = Seat.of(spyScreen, "A2", true, 1, 2);

        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));
        when(screeningRepository.existsByScreenIdAndScreeningStatusNot(
                screenId, ScreeningStatus.FINISHED))
                .thenReturn(true);

        // when & then
        assertThrows(BusinessException.class,
                () -> seatService.updateSeatActive(seatId, false));

        // 상태 변경이 일어나면 안 됨
        assertTrue(seat.isActive());

        verify(seatRepository).findById(seatId);
        verify(screeningRepository)
                .existsByScreenIdAndScreeningStatusNot(screenId, ScreeningStatus.FINISHED);
        verifyNoInteractions(seatMapper);
    }
}
