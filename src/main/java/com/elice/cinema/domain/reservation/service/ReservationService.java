package com.elice.cinema.domain.reservation.service;

import com.elice.cinema.domain.member.entity.Member;
import com.elice.cinema.domain.member.repository.MemberRepository;
import com.elice.cinema.domain.movie.dto.response.ReservationMovieSelectResponse;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.mapper.MovieMapper;
import com.elice.cinema.domain.movie.repository.MovieRepository;
import com.elice.cinema.domain.movieImage.repository.MovieImageRepository;
import com.elice.cinema.domain.policy.service.EnvironmentPolicyService;
import com.elice.cinema.domain.reservation.dto.CancelReservationInfoDto;
import com.elice.cinema.domain.reservation.dto.response.ReservationCheckoutResponse;
import com.elice.cinema.domain.reservation.dto.response.ReservationIdResponse;
import com.elice.cinema.domain.reservation.dto.response.TossPaymentReservationResponse;
import com.elice.cinema.domain.reservation.entity.Reservation;
import com.elice.cinema.domain.reservation.entity.ReservationStatus;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {
    private static final int DAYS_RANGE_INCLUSIVE = 6; //TODO: 이것도 환경 변수 테이블에 넣을지 고민

    @Value("${toss.payments.client-key}")
    private String tossClientKey;

    private final MovieRepository movieRepository;
    private final ScreeningRepository screeningRepository;
    private final ReservedSeatRepository reservedSeatRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationLockRepository reservationLockRepository;
    private final MemberRepository memberRepository;
    private final SeatRepository seatRepository;
    private final MovieImageRepository movieImageRepository;

    private final EnvironmentPolicyService environmentPolicyService;
    private final SeatHoldProperties seatHoldProperties;
    private final MovieMapper movieMapper;
    private final ScreeningMapper screeningMapper;
    private final ReservationMapper reservationMapper;

    @Transactional
    public Long holdSeats(Long screeningId, List<Long> seatIds, Long memberId) {
        validateSeatCount(seatIds);
        validateBookable(screeningId, seatIds);

        final int holdMinutes = seatHoldProperties.getMinutes();
        final int graceMinutes = seatHoldProperties.getRedisGraceMinutes();

        List<Seat> seats = getSeats(seatIds);
        Screening screening = getScreeningWithMovieAndScreen(screeningId);
        Member member = getMember(memberId);

        // 선택한 좌석에 redis lock 처리
        List<Long> locked = new ArrayList<>();
        try {
            for (Long seatId : seatIds) {
                boolean ok = reservationLockRepository.lock(  // FIXME: seatId들을 한 번에 모아서 lock 걸도록 수정하기
                        screeningId,
                        seatId,
                        memberId,
                        holdMinutes + graceMinutes,
                        TimeUnit.MINUTES
                );
                if (!ok) {  // 이미 lock이 걸린 좌석을 선택한 경우 이전에 lock 걸었던 좌석들의 lock을 풀어주고 예외를 던짐
                    throw new BusinessException(ErrorCode.SEAT_ALREADY_HELD);
                }
                locked.add(seatId);  // 성공한 것만 기록
            }

            int totalPrice = calculateTotalPrice(seats);
            Reservation reservation = Reservation.createHoldReservation(screening, member, totalPrice, Duration.ofMinutes(holdMinutes));
            Reservation savedReservation = reservationRepository.save(reservation);

            List<ReservedSeat> reservedSeats = seats.stream()
                    .map(seat -> ReservedSeat.createHoldReservedSeat(screening, seat))
                    .peek(reservation::addReservedSeat)  // 양방향 세팅
                    .toList();
            reservedSeatRepository.saveAll(reservedSeats);  // FIXME: ReservedSeat의 PK 전략이 현재 IDENTIFY -> saveAll 날리면 컬렉션처럼 루프로 하나씩 insert됨

            return savedReservation.getId();
        } finally {  // 성공하든 실패하든 lock은 반드시 반환해줘야 함
            // 내가 잡은 redis lock만 해제
            reservationLockRepository.unlockAll(screeningId, locked);
        }
    }

    @Transactional
    public void cancelHoldReservation(Long reservationId, Long memberId) {
        CancelReservationInfoDto target = reservationRepository.findCancelReservationInfo(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        if(!target.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.RESERVATION_FORBIDDEN);
        }

        if (target.getStatus() == ReservationStatus.CONFIRMED) {  // 이미 확정된 예매는 삭제 불가
            throw new BusinessException(ErrorCode.RESERVATION_ALREADY_CONFIRMED);
        }

        if (target.getStatus() != ReservationStatus.HOLD) {  // EXPIRED나 CANCELED면 처리해줄 것이 없는 상태
            return;
        }

        reservationRepository.deleteById(reservationId);  // HOLD라면 삭제 - 연관된 reservedSeat들은 delete cascade로 삭제됨
    }

    public int calculateTotalPrice(List<Seat> seats) {
        return environmentPolicyService.getDefaultPrice() * seats.size();
    }

    public ReservationIdResponse getReservationIdByReservationCode(String orderId) {
        Reservation reservation = reservationRepository.findByReservationCode(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        return reservationMapper.toReservationIdResponse(reservation);
    }

    // 좌석 개수 검증 (선택한 좌석의 개수가 개인이 예매할 수 있는 최대 좌석수를 넘기지 않았는지 검증)
    private void validateSeatCount(List<Long> seatIds) {
        int max = environmentPolicyService.getMaxReservationCount();
        if (seatIds == null || seatIds.isEmpty() || seatIds.size() > max) {
            throw new BusinessException(ErrorCode.RESERVATION_SEAT_LIMIT_EXCEEDED);
        }
    }

    // 요청으로 들어온 좌석들이 실제로 예매 가능한지 검사 (이미 선점되거나 예매된 좌석인지 검사)
    private void validateBookable(Long screeningId, List<Long> seatIds) {
        List<Long> blocked = reservedSeatRepository.findBlockedSeatIdsIn(screeningId, seatIds, ReservationStatus.blocked());

        if (!blocked.isEmpty()) {  // 이미 선점되었거나 예매된 좌석이 포함된 요청임을 의미
            throw new BusinessException(ErrorCode.SEAT_ALREADY_HELD);
        }
    }

    public List<ReservationMovieSelectResponse> getMoviesHavingScreeningsInDateRange() {
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

        // n+1을 방지하기 위해 상영들의 예약 좌석들을 한번에 쿼리로 가져와 Map으로 관리
        Map<Long, Long> reservedCountMap = getReservedCountByScreeningIdMap(screenings);

        return screenings.stream()
                .map(screening -> {
                    int remainingSeats = calculateRemainingSeats(screening, reservedCountMap);
                    return screeningMapper
                            .toReservationScheduleResponse(screening, remainingSeats);
                })
                .toList();
    }

    public ReservationCheckoutResponse getCheckoutPage(Long reservationId) {
        Reservation reservation = reservationRepository.findByIdWithScreeningAndMovie(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        Movie movie = reservation.getScreening().getMovie();

        String movieThumbnail = movieImageRepository.findThumbnailUrlByMovieId(movie.getId())
                .orElse(null);
        /*String movieThumbnail = movieImageRepository.findThumbnailUrlByMovieId(movie.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_THUMBNAIL_NOT_FOUND));*/
        // TODO: 데이터로 영화 썸네일 넣고 다시 시도하기, 예매 생성 생기면 다시 시도
        List<String> seatCodes = reservedSeatRepository.findSeatCodesByReservationId(reservationId);

        return reservationMapper.toReservationCheckoutResponse(reservation, movieThumbnail, seatCodes);
    }

    public TossPaymentReservationResponse getTossPage(Long reservationId) {
        Reservation reservation = reservationRepository.findByIdAndStatus(reservationId, ReservationStatus.HOLD)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        String orderId = reservation.getReservationCode();
        return reservationMapper.toPaymentReservationResponse(reservation, orderId, tossClientKey);
    }

    private Screening getScreeningWithMovieAndScreen(Long screeningId) {  // ScreeningService로 메서드 위치 이동해뒀습니다.
        return screeningRepository.findByIdWithMovieAndScreen(screeningId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCREENING_NOT_FOUND));
    }

    private List<Seat> getSeats(List<Long> seatIds) {
        List<Seat> seats = seatRepository.findAllById(seatIds);

        if (seats.size() != seatIds.size()) {
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
        if (seats.stream().anyMatch(seat -> !seat.isActive())) {
            throw new BusinessException(ErrorCode.SEAT_INACTIVE);
        }
    }

    private Map<Long, Long> getReservedCountByScreeningIdMap(List<Screening> screenings) {
        if (screenings.isEmpty()) return Collections.emptyMap();

        List<Long> screeningIds = screenings.stream().map(Screening::getId).toList();

        return reservedSeatRepository.countByScreeningIds(screeningIds).stream()
                .collect(Collectors.toMap(
                        ReservedSeatRepository.ReservedCountRow::getScreeningId,
                        ReservedSeatRepository.ReservedCountRow::getReservedCount
                ));
    }

    private int calculateRemainingSeats(Screening sc, Map<Long, Long> reservedCountMap) {
        int totalSeats = sc.getScreen().getTotalSeats();
        long reservedCount = reservedCountMap.getOrDefault(sc.getId(), 0L);
        return (int) (totalSeats - reservedCount); // 방어적으로 0 미만 방지
    }
}
