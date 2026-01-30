package com.elice.cinema.domain.screen.service;

import com.elice.cinema.domain.screen.dto.request.ScreenCreateRequest;
import com.elice.cinema.domain.screen.dto.request.ScreenUpdateRequest;
import com.elice.cinema.domain.screen.dto.response.ScreenDetailResponse;
import com.elice.cinema.domain.screen.dto.response.ScreenListResponse;
import com.elice.cinema.domain.screen.entity.Screen;
import com.elice.cinema.domain.screen.mapper.ScreenMapper;
import com.elice.cinema.domain.screen.repository.ScreenRepository;
import com.elice.cinema.domain.screen.repository.SeatRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
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
    private final SeatRepository seatRepository;
    private final ScreenMapper screenMapper;
    private final ScreenValidator screenValidator;

    public Page<ScreenListResponse> getScreens(Boolean operating, Pageable pageable) {
        if (operating == null) {
            return screenRepository.findAll(pageable)
                    .map(screenMapper::toScreenListResponse);
        }

        return screenRepository.findByOperating(operating, pageable)
                .map(screenMapper::toScreenListResponse);
    }

    public ScreenDetailResponse getScreenDetail(Long screenId) {
        Screen screen = findScreenById(screenId);

        return screenMapper.toScreenDetailResponse(screen);
    }

    public int getAvailableSeatCount(Long screenId) {
        // active=true 인 좌석 수 = 사용 가능 좌석 수
        return (int) seatRepository.countByScreenIdAndActiveTrue(screenId);
    }

    @Transactional
    public void createScreen(ScreenCreateRequest req) {
        screenValidator.validateCreate(req);

        Screen screen = screenMapper.toEntity(req);

        req.getSeats().forEach(seat ->
                screen.addSeat(seat.getSeatCode(), seat.getActive(), seat.getRowNo(), seat.getColNo())
        );

        screenRepository.save(screen);
    }

    public ScreenUpdateRequest getScreenUpdateForm(Long screenId) {
        Screen screen = findScreenById(screenId);

        return screenMapper.toScreenUpdateRequest(screen);
    }

    @Transactional
    public void updateScreen(Long screenId, ScreenUpdateRequest req) {
        Screen screen = findScreenById(screenId);

        // 1. 변경 사항 검증 (정책 검증)
        screenValidator.validateUpdate(screen, req);

        // 2. 엔티티 값 변경 (Dirty Checking)
        screen.updateAll(
                req.getName(),
                req.getScreeningType(),
                req.getOperating()
        );
    }

    // === Helper Methods ===
    private Screen findScreenById(Long screenId) {
        return screenRepository.findById(screenId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCREEN_NOT_FOUND));
    }
}
