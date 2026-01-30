package com.elice.cinema.domain.screening.repository;

import com.elice.cinema.domain.screening.dto.request.AdminScreeningSearchRequest;
import com.elice.cinema.domain.screening.dto.response.AdminScreeningFilterOptionResponse;
import com.elice.cinema.domain.screening.entity.Screening;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ScreeningRepositoryCustom {

    Page<Screening> searchAdmin(AdminScreeningSearchRequest request, Pageable pageable);

    List<AdminScreeningFilterOptionResponse> findAdminScreeningMovieFilterOptions();

    List<AdminScreeningFilterOptionResponse> findAdminScreeningScreenFilterOptions();
}
