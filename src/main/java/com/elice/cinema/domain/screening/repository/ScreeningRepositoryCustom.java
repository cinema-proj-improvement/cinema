package com.elice.cinema.domain.screening.repository;

import com.elice.cinema.domain.screening.dto.request.AdminScreeningSearchRequest;
import com.elice.cinema.domain.screening.dto.response.AdminScreeningFilterOptionResponse;
import com.elice.cinema.domain.screening.dto.response.AdminScreeningSeatResponse;
import com.elice.cinema.domain.screening.dto.response.AdminScreeningSeatSummaryResponse;
import com.elice.cinema.domain.screening.entity.Screening;
import com.querydsl.core.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ScreeningRepositoryCustom {

    Page<Screening> searchAdmin(AdminScreeningSearchRequest request, Pageable pageable);

    List<AdminScreeningFilterOptionResponse> findAdminScreeningMovieFilterOptions();

    List<AdminScreeningFilterOptionResponse> findAdminScreeningScreenFilterOptions();

    // 상영별 좌석 현황 조회
    List<AdminScreeningSeatResponse> findAdminSeatsByScreeningId(Long screeningId);

    // 좌석 요약
    AdminScreeningSeatSummaryResponse findAdminSeatSummaryByScreeningId(Long screeningId);

    List<Tuple> findSeatSummaryByScreeningIds(List<Long> screeningIds);
}
