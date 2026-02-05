package com.elice.cinema.domain.screen.service;

import com.elice.cinema.domain.screen.dto.request.ScreenCreateRequest;
import com.elice.cinema.domain.screen.dto.request.ScreenUpdateRequest;
import com.elice.cinema.domain.screen.dto.request.SeatCreateRequest;
import com.elice.cinema.domain.screen.entity.Screen;
import com.elice.cinema.domain.screen.repository.ScreenRepository;
import com.elice.cinema.domain.screening.entity.ScreeningStatus;
import com.elice.cinema.domain.screening.repository.ScreeningRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

//DB 조회 필요 / 다른 필드와의 관계 / 중복 / 규칙 같은 비즈니스 검증은 서비스 단에서 유효성 검증이 핑요

@Component
@RequiredArgsConstructor
public class ScreenValidator {
    private final ScreenRepository screenRepository;
    private final ScreeningRepository screeningRepository;

    //TODO: 2팀 Validator 형식 확인하고 수정 및 공부하기
    /* 상영관 생성 공통 검증 */
    public void validateCreate(ScreenCreateRequest request) {
        validateScreenNameUnique(request.getName());
        validateSeatCountMatches(request.getTotalSeats(), request.getSeats());
        validateSeatUniqueness(request.getSeats());
    }

    /* 상영관 수정 공통 검증 */
    public void validateUpdate(Screen screen, ScreenUpdateRequest request) {
        validateNoActiveScreenings(screen.getId());
        validateScreenNameUniqueOnUpdate(screen.getName(), request.getName());
    }

    /* 운영중인 상영(SCHEDULED 포함) 여부 검증*/
    private void validateNoActiveScreenings(Long screenId) {
        if (screeningRepository.existsByScreenIdAndScreeningStatusNot(screenId, ScreeningStatus.FINISHED)) {
            throw new BusinessException(
                    ErrorCode.SCREEN_UPDATE_NOT_ALLOWED_WHEN_SCREENING_ACTIVE
            );
        }
    }

    /* 상영관 이름 중복 검증 */
    private void validateScreenNameUnique(String name) {
        // 동시성 문제가 발생할 수 있음 (필요시 LOCK)
        if (screenRepository.existsByName(name)) {
            throw new BusinessException(ErrorCode.SCREEN_NAME_DUPLICATED);
        }
    }

    /* 수정 시 상영관 이름 중복 검증 (본인 제외) */
    private void validateScreenNameUniqueOnUpdate(String oldName, String newName) {
        if (!oldName.equals(newName) && screenRepository.existsByName(newName)) {
            throw new BusinessException(ErrorCode.SCREEN_NAME_DUPLICATED);
        }
    }

    /* 총 좌석수 == 좌석 리스트 수 */
    private void validateSeatCountMatches(Integer totalSeats, List<SeatCreateRequest> seats) {
        if (seats == null || seats.isEmpty()) {
            throw new BusinessException(ErrorCode.SCREEN_SEAT_REQUIRED);
        }
        if (!totalSeats.equals(seats.size())) {
            throw new BusinessException(ErrorCode.SCREEN_SEAT_COUNT_MISMATCH);
        }
    }

    /* 좌석 중복 검증 (추천: (rowNo, colNo) 중복 + seatCode 중복 둘 다 체크) */
    private void validateSeatUniqueness(List<SeatCreateRequest> seats) {
        Set<String> posSet = new HashSet<>();
        Set<String> codeSet = new HashSet<>();

        for (SeatCreateRequest s : seats) {
            String posKey = s.getRowNo() + "-" + s.getColNo();
            if (!posSet.add(posKey)) {
                throw new BusinessException(ErrorCode.SCREEN_DUPLICATE_SEAT_POSITION);
            }

            String code = s.getSeatCode();
            if (code != null && !codeSet.add(code)) {
                throw new BusinessException(ErrorCode.SCREEN_DUPLICATE_SEAT_CODE);
            }
        }
    }
}
