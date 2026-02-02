package com.elice.cinema.domain.reservation.service;

import com.elice.cinema.domain.member.entity.Member;
import com.elice.cinema.domain.member.repository.MemberRepository;
import com.elice.cinema.domain.policy.service.EnvironmentPolicyService;
import com.elice.cinema.domain.reservation.entity.Reservation;
import com.elice.cinema.domain.reservation.entity.ReservedSeat;
import com.elice.cinema.domain.reservation.repository.ReservationLockRepository;
import com.elice.cinema.domain.reservation.repository.ReservationRepository;
import com.elice.cinema.domain.reservation.repository.ReservedSeatRepository;
import com.elice.cinema.domain.screen.entity.Seat;
import com.elice.cinema.domain.screen.repository.SeatRepository;
import com.elice.cinema.domain.screening.entity.Screening;
import com.elice.cinema.domain.screening.repository.ScreeningRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final ReservedSeatRepository reservedSeatRepository;
    private final ReservationLockRepository reservationLockRepository;

    private final ScreeningRepository screeningRepository;
    private final MemberRepository memberRepository;
    private final SeatRepository seatRepository;

    private final EnvironmentPolicyService environmentPolicyService;

    @Transactional
    public Long holdSeats(Long screeningId, List<Long> seatIds, Long memberId) {
        validateSeatCount(seatIds);

        List<Seat> seats = getSeats(seatIds);
        Screening screening = getScreeningWithMovieAndScreen(screeningId);
        Member member = getMember(memberId);

        int ttl = environmentPolicyService.getReservationTTL();

        // 선택한 좌석에 redis lock 처리
        List<Long> locked = new ArrayList<>();
        for(Long seatId : seatIds) {
            boolean ok = reservationLockRepository.lock(screeningId, seatId, memberId, ttl, TimeUnit.MINUTES);
            if(!ok) {  // 이미 lock이 걸린 좌석을 선택한 경우 이전에 lock 걸었던 좌석들의 lock을 풀어주고 예외를 던짐
                for(Long lockedSeatId : locked) {
                    reservationLockRepository.unlock(screeningId, lockedSeatId);
                }
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);  // FIXME: SEAT_ALREADY_HELD로 바꾸기
            }
            locked.add(seatId);
        }

        int totalPrice = calculateTotalPrice(seats);
        try {
            Reservation reservation = Reservation.createHoldReservation(screening, member, totalPrice, Duration.ofMinutes(ttl));
            Reservation savedReservation = reservationRepository.save(reservation);

            List<ReservedSeat> reservedSeats = seats.stream()
                    .map(seat -> ReservedSeat.createHoldReservedSeat(reservation, screening, seat))
                    .toList();
            reservedSeatRepository.saveAll(reservedSeats);

            return savedReservation.getId();

        } catch (RuntimeException e) {
            // DB 실패 시 redis lock도 해제
            reservationLockRepository.unlockAll(screeningId, seatIds);
            throw e;
        }
    }

    public int calculateTotalPrice(List<Seat> seats) {  // TODO: 이후 가격 계산에 대한 로직이 복잡해지면 클래스로 분리합니다. (현재도 위치가 적절하진 않음)
        int totalPrice = 0;
        for(int i = 0; i < seats.size(); i++) {
            totalPrice += environmentPolicyService.getDefaultPrice();
        }
        return totalPrice;
    }

    // 좌석 개수 검증 (선택한 좌석의 개수가 개인이 예매할 수 있는 최대 좌석수를 넘기지 않았는지 검증)
    private void validateSeatCount(List<Long> seatIds) {
        int max = environmentPolicyService.getMaxReservationCount();
        if (seatIds == null || seatIds.isEmpty() || seatIds.size() > max) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);  // FIXME: 에러코드 추가해서 수정하기
        }
    }

    private Screening getScreeningWithMovieAndScreen(Long screeningId) {
        return screeningRepository.findByIdWithMovieAndScreen(screeningId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCREENING_NOT_FOUND));
    }

    private List<Seat> getSeats(List<Long> seatIds) {
        List<Seat> seats = seatRepository.findAllById(seatIds);

        if(seats.size() != seatIds.size()) {
            throw new BusinessException(ErrorCode.SEAT_NOT_FOUND);
        }

        validateSeatsAreValid(seats);

        return seats;
    }

    private Member getMember(Long memberId) {  // TODO: reservation service에서 memberRepo 가지고 이거 처리하는 게 이상함. 리팩토링 대안 생각해보기
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void validateSeatsAreValid(List<Seat> seats) {
        if(seats.stream().anyMatch(seat -> !seat.isActive())) {
            throw new BusinessException(ErrorCode.SEAT_INACTIVE);
        }
    }
}
