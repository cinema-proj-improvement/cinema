package com.elice.cinema.domain.reservation.service;

import com.elice.cinema.domain.policy.service.EnvironmentPolicyService;
import com.elice.cinema.domain.reservation.dto.response.seatselection.ScreenInfo;
import com.elice.cinema.domain.reservation.dto.response.seatselection.ScreeningInfo;
import com.elice.cinema.domain.reservation.dto.response.seatselection.SeatInfo;
import com.elice.cinema.domain.reservation.dto.response.seatselection.SeatSelectionResponse;
import com.elice.cinema.domain.reservation.entity.ReservationStatus;
import com.elice.cinema.domain.screen.entity.Seat;
import com.elice.cinema.domain.screen.mapper.ScreenMapper;
import com.elice.cinema.domain.screen.mapper.SeatMapper;
import com.elice.cinema.domain.screen.service.SeatService;
import com.elice.cinema.domain.screening.entity.Screening;
import com.elice.cinema.domain.screening.mapper.ScreeningMapper;
import com.elice.cinema.domain.screening.service.ScreeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatSelectionFacade {
    private final ScreeningService screeningService;
    private final SeatService seatService;
    private final ReservedSeatService reservedSeatService;
    private final EnvironmentPolicyService environmentPolicyService;

    private final SeatMapper seatMapper;
    private final ScreeningMapper screeningMapper;
    private final ScreenMapper screenMapper;

    public SeatSelectionResponse getSeatSelectionPageInfo(Long screeningId) {
        Screening screening = screeningService.getScreeningWithMovieAndScreen(screeningId);

        List<Seat> seats = seatService.getSeatsByScreenId(screening.getScreen().getId());

        Set<Long> blockedSeatIds = new HashSet<>(
                reservedSeatService.getBlockedSeatIds(screeningId, ReservationStatus.blocked())
        );  // List보단 Set이 contains() 메서드의 성능에서 유리합니다. (hash)

        int maxReservationCount = environmentPolicyService.getMaxReservationCount();
        int defaultPrice = environmentPolicyService.getDefaultPrice();

        List<SeatInfo> seatInfos = seats.stream()
                .map(seat -> seatMapper.toSeatInfo(seat, blockedSeatIds))
                .toList();

        ScreeningInfo screeningInfo = screeningMapper.toScreeningInfo(screening);

        ScreenInfo screenInfo = screenMapper.toScreenInfo(screening.getScreen());

        return SeatSelectionResponse.of(screeningInfo, screenInfo, seatInfos, maxReservationCount, defaultPrice);
    }

}
