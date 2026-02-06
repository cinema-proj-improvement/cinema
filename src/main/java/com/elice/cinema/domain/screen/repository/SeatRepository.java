package com.elice.cinema.domain.screen.repository;

import com.elice.cinema.domain.screen.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    long countByScreenIdAndActiveTrue(Long screenId);

    // 해당 상영관의 모든 좌석 조회
    List<Seat> findAllByScreenId(Long screenId);
}
