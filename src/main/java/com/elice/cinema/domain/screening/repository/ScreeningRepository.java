package com.elice.cinema.domain.screening.repository;

import com.elice.cinema.domain.screening.entity.Screening;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreeningRepository extends JpaRepository<Screening, Long>, ScreeningRepositoryCustom {
}
