package com.elice.cinema.domain.screening.repository;

import com.elice.cinema.domain.screening.dto.request.AdminScreeningSearchRequest;
import com.elice.cinema.domain.screening.entity.Screening;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminScreeningQueryRepository {

    // 관리자 / 사용자 공통 상영 조회
    Page<Screening> search(AdminScreeningSearchRequest condition, Pageable pageable);
}
