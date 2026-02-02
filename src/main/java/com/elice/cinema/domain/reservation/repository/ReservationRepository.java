package com.elice.cinema.domain.reservation.repository;

import com.elice.cinema.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
