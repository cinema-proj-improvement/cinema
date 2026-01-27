package com.elice.cinema.domain.screen.repository;

import com.elice.cinema.domain.screen.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    long countByScreenIdAndActiveTrue(Long screenId);
}
