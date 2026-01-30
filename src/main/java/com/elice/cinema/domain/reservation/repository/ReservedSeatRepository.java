package com.elice.cinema.domain.reservation.repository;

import com.elice.cinema.domain.reservation.entity.ReservedSeat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservedSeatRepository extends JpaRepository<ReservedSeat, Long> {
    int countAllByScreening_Id(Long screeningId);
}
