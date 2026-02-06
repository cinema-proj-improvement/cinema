package com.elice.cinema.domain.reservation.service;

import com.elice.cinema.domain.reservation.entity.ReservationStatus;
import com.elice.cinema.domain.reservation.repository.ReservedSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservedSeatService {
    private final ReservedSeatRepository reservedSeatRepository;

    public List<Long> getBlockedSeatIds(Long screeningId, List<ReservationStatus> blockedCondition) {
        return reservedSeatRepository.findBlockedSeatIds(screeningId, blockedCondition);
    }
}
