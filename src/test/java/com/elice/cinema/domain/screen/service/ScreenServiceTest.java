package com.elice.cinema.domain.screen.service;

import com.elice.cinema.domain.common.ScreeningType;
import com.elice.cinema.domain.screen.dto.request.ScreenUpdateRequest;
import com.elice.cinema.domain.screen.entity.Screen;
import com.elice.cinema.domain.screen.mapper.ScreenMapper;
import com.elice.cinema.domain.screen.repository.ScreenRepository;
import com.elice.cinema.domain.screen.repository.SeatRepository;
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
class ScreenServiceTest {

    @Mock ScreenRepository screenRepository;
    @Mock SeatRepository seatRepository;
    @Mock ScreenMapper screenMapper;
    @Mock ScreenValidator screenValidator;

    @InjectMocks ScreenService screenService;

    @Test
    @DisplayName("상영관 비활성화 성공 - 진행 중 상영이 없으면 operating=false로 변경된다")
    void deactivateScreen_success() {
        // given
        Long screenId = 1L;
        Screen screen = Screen.of("1관", ScreeningType.TWO_D, 10, true);

        ScreenUpdateRequest req = mock(ScreenUpdateRequest.class);
        when(req.getName()).thenReturn("1관");                    // 이름 동일
        when(req.getScreeningType()).thenReturn(ScreeningType.TWO_D);
        when(req.getOperating()).thenReturn(false);               // ✅ 비활성화

        when(screenRepository.findById(screenId)).thenReturn(Optional.of(screen));
        // validateUpdate는 통과한다고 가정 (예외 안 던짐)
        doNothing().when(screenValidator).validateUpdate(screen, req);

        // when
        screenService.updateScreen(screenId, req);

        // then
        assertFalse(screen.isOperating());

        verify(screenRepository).findById(screenId);
        verify(screenValidator).validateUpdate(screen, req);
        verifyNoInteractions(seatRepository, screenMapper);
    }

    @Test
    @DisplayName("상영관 비활성화 실패 - 진행 중(종료되지 않은) 상영이 존재하면 수정 불가")
    void deactivateScreen_fail_whenActiveScreeningExists() {
        // given
        Long screenId = 1L;
        Screen screen = Screen.of("1관", ScreeningType.TWO_D, 10, true);

        ScreenUpdateRequest req = mock(ScreenUpdateRequest.class);

        when(screenRepository.findById(screenId)).thenReturn(Optional.of(screen));

        doThrow(BusinessException.class).when(screenValidator).validateUpdate(screen, req);

        // when & then
        assertThrows(BusinessException.class, () -> screenService.updateScreen(screenId, req));

        // 상태는 바뀌면 안 됨
        assertTrue(screen.isOperating());

        verify(screenRepository).findById(screenId);
        verify(screenValidator).validateUpdate(screen, req);
        verifyNoInteractions(seatRepository, screenMapper);
    }

    @Test
    @DisplayName("상영관 비활성화 실패 - 변경하려는 상영관 이름이 이미 존재하면 수정 불가(이름 중복)")
    void deactivateScreen_fail_whenNameDuplicated() {
        // given
        Long screenId = 1L;
        Screen screen = Screen.of("1관", ScreeningType.TWO_D, 10, true);

        ScreenUpdateRequest req = mock(ScreenUpdateRequest.class);

        when(screenRepository.findById(screenId)).thenReturn(Optional.of(screen));
        doThrow(BusinessException.class).when(screenValidator).validateUpdate(screen, req);

        // when & then
        assertThrows(BusinessException.class, () -> screenService.updateScreen(screenId, req));

        // 상태는 바뀌면 안 됨
        assertTrue(screen.isOperating());

        verify(screenRepository).findById(screenId);
        verify(screenValidator).validateUpdate(screen, req);
        verifyNoInteractions(seatRepository, screenMapper);
    }
}
