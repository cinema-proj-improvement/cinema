package com.elice.cinema.domain.screening.service;

import com.elice.cinema.domain.screening.dto.request.AdminScreeningSearchRequest;
import com.elice.cinema.domain.screening.dto.response.AdminScreeningResponse;
import com.elice.cinema.domain.screening.entity.Screening;
import com.elice.cinema.domain.screening.entity.ScreeningStatus;
import com.elice.cinema.domain.screening.mapper.ScreeningMapper;
import com.elice.cinema.domain.screening.repository.ScreeningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ScreeningService {
    private final ScreeningRepository screeningRepository;
    private final ScreeningMapper screeningMapper;

    public Page<AdminScreeningResponse> searchAdmin(
            AdminScreeningSearchRequest request,
            Pageable pageable
    ) {
        applyDefaultDateRange(request);

        LocalDateTime now = LocalDateTime.now();

        return screeningRepository
                .searchAdmin(request, pageable)
                .map(screening -> toAdminResponse(screening, now));
    }


    // 헬퍼 메서드
    private void applyDefaultDateRange(AdminScreeningSearchRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            LocalDate today = LocalDate.now();
            request.setStartDate(today);
            request.setEndDate(today.plusDays(7));
        }
    }

    private AdminScreeningResponse toAdminResponse(
            Screening screening,
            LocalDateTime now
    ) {
        ScreeningStatus status = calculateStatus(screening, now);

        AdminScreeningResponse base =
                screeningMapper.toAdminResponse(screening);

        return new AdminScreeningResponse(
                base.getId(),
                base.getDate(),
                base.getStartTime(),
                base.getEndTime(),
                base.getMovieTitle(),
                base.getScreenName(),
                base.getScreeningType(),
                status
        );
    }

    // 상태 계산 표시용
    private ScreeningStatus calculateStatus(
            Screening screening,
            LocalDateTime now
    ) {
        // 관리자 취소 최우선
        if (screening.getScreeningStatus() == ScreeningStatus.CANCELED) {
            return ScreeningStatus.CANCELED;
        }

        // 아직 시작 전
        if (now.isBefore(screening.getStartAt())) {
            return ScreeningStatus.SCHEDULED;
        }

        // 상영 중
        if (now.isBefore(screening.getEndAt())) {
            return ScreeningStatus.OPEN;
        }

        // 종료
        return ScreeningStatus.FINISHED;
    }
}