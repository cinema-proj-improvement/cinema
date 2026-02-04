package com.elice.cinema.domain.reservation.service;

import com.elice.cinema.domain.member.entity.Member;
import com.elice.cinema.domain.member.repository.MemberRepository;
import com.elice.cinema.domain.movie.dto.response.ReservationMovieSelectResponse;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.mapper.MovieMapper;
import com.elice.cinema.domain.movie.repository.MovieRepository;
import com.elice.cinema.domain.movieImage.repository.MovieImageRepository;
import com.elice.cinema.domain.policy.service.EnvironmentPolicyService;
import com.elice.cinema.domain.reservation.dto.response.ReservationCheckoutResponse;
import com.elice.cinema.domain.reservation.entity.Reservation;
import com.elice.cinema.domain.reservation.entity.ReservedSeat;
import com.elice.cinema.domain.reservation.mapper.ReservationMapper;
import com.elice.cinema.domain.reservation.repository.ReservationLockRepository;
import com.elice.cinema.domain.reservation.repository.ReservationRepository;
import com.elice.cinema.domain.reservation.repository.ReservedSeatRepository;
import com.elice.cinema.domain.screen.entity.Seat;
import com.elice.cinema.domain.screen.repository.SeatRepository;
import com.elice.cinema.domain.screening.dto.response.ReservationScheduleResponse;
import com.elice.cinema.domain.screening.entity.Screening;
import com.elice.cinema.domain.screening.mapper.ScreeningMapper;
import com.elice.cinema.domain.screening.repository.ScreeningRepository;
import com.elice.cinema.global.config.properties.SeatHoldProperties;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {
    private static final int DAYS_RANGE_INCLUSIVE = 6; //TODO: 이것도 환경 변수 테이블에 넣을지 고민

    private final MovieRepository movieRepository;
    private final ReservedSeatRepository reservedSeatRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationLockRepository reservationLockRepository;
    private final MovieImageRepository movieImageRepository;
    private final ScreeningRepository screeningRepository;
    private final MemberRepository memberRepository;
    private final SeatRepository seatRepository;

    private final EnvironmentPolicyService environmentPolicyService;
    private final SeatHoldProperties seatHoldProperties;
    private final MovieMapper movieMapper;
    private final ScreeningMapper screeningMapper;
    private final ReservationMapper reservationMapper;

    @Transactional
    public Long holdSeats(Long screeningId, List<Long> seatIds, Long memberId) {  // TODO: 메서드 분리 고려하기
        validateSeatCount(seatIds);

        final int holdMinutes = seatHoldProperties.getMinutes();
        final int graceMinutes = seatHoldProperties.getRedisGraceMinutes();

        List<Seat> seats = getSeats(seatIds);
        Screening screening = getScreeningWithMovieAndScreen(screeningId);
        Member member = getMember(memberId);

        // 선택한 좌석에 redis lock 처리
        List<Long> locked = new ArrayList<>();
        for(Long seatId : seatIds) {
            boolean ok = reservationLockRepository.lock(screeningId, seatId, memberId, holdMinutes + graceMinutes, TimeUnit.MINUTES);
            if(!ok) {  // 이미 lock이 걸린 좌석을 선택한 경우 이전에 lock 걸었던 좌석들의 lock을 풀어주고 예외를 던짐
                for(Long lockedSeatId : locked) {
                    reservationLockRepository.unlock(screeningId, lockedSeatId);
                }
                throw new BusinessException(ErrorCode.SEAT_ALREADY_HELD);
            }
            locked.add(seatId);
        }

        int totalPrice = calculateTotalPrice(seats);
        try {
            Reservation reservation = Reservation.createHoldReservation(screening, member, totalPrice, Duration.ofMinutes(holdMinutes));
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

    public int calculateTotalPrice(List<Seat> seats) {  // TODO: 이후 가격 계산에 대한 로직이 복잡해지면 클래스로 분리합니다. (현재도 위치 적절하지 않음)
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
            throw new BusinessException(ErrorCode.RESERVATION_SEAT_LIMIT_EXCEEDED);
        }
    }

    public List<ReservationMovieSelectResponse> getMoviesWithScreeningsWithin() {
        LocalDate today = LocalDate.now();

        LocalDateTime from = today.atStartOfDay();
        LocalDateTime toExclusive = today.plusDays(DAYS_RANGE_INCLUSIVE + 1).atStartOfDay();

        List<Movie> movies = movieRepository.findDistinctMoviesHavingScreeningsBetween(from, toExclusive);

        return movies.stream()
                .map(movieMapper::toReservationMovieSelectResponse)
                .toList();
    }

    public List<ReservationScheduleResponse> getSchedulesByDate(LocalDate date, Long movieId) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime toExclusive = date.plusDays(1).atStartOfDay();

        List<Screening> screenings = screeningRepository.findSchedulesByDate(
                from,
                toExclusive,
                movieId
        );

        return screenings.stream()
                .map(screening -> {
                    Integer remainingSeats = calculateRemainingSeats(screening);
                    return screeningMapper
                            .toReservationScheduleResponse(screening, remainingSeats);
                })
                .toList();
    }

    public ReservationCheckoutResponse getCheckoutPage(Long reservationId) {
        Reservation reservation = reservationRepository.findByIdWithScreeningAndMovie(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        Movie movie = reservation.getScreening().getMovie();

        String movieThumbnail = /*movieImageRepository.findThumbnailUrlByMovieId(movie.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_THUMBNAIL_NOT_FOUND));*/ null;
        // TODO: 데이터로 영화 썸네일 넣고 다시 시도하기, 예매 생성 생기면 다시 시도
        List<String> seatCodes = reservedSeatRepository.findSeatCodesByReservationId(reservationId);

        return reservationMapper.toReservationCheckoutResponse(reservation, movieThumbnail, seatCodes);
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

    private Integer calculateRemainingSeats(Screening screening) {
        int totalSeats = screening.getScreen().getTotalSeats();
        int reservedCount = reservedSeatRepository.countAllByScreening_Id(screening.getId());

        return totalSeats - reservedCount;
    }
}
