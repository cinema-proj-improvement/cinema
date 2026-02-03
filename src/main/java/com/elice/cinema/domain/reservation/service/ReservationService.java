package com.elice.cinema.domain.reservation.service;

import com.elice.cinema.domain.movie.dto.response.ReservationMovieSelectResponse;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.mapper.MovieMapper;
import com.elice.cinema.domain.movie.repository.MovieRepository;
import com.elice.cinema.domain.movieImage.repository.MovieImageRepository;
import com.elice.cinema.domain.reservation.dto.response.TossPaymentReservationResponse;
import com.elice.cinema.domain.reservation.dto.response.ReservationCheckoutResponse;
import com.elice.cinema.domain.reservation.entity.Reservation;
import com.elice.cinema.domain.reservation.mapper.ReservationMapper;
import com.elice.cinema.domain.reservation.repository.ReservationRepository;
import com.elice.cinema.domain.reservation.repository.ReservedSeatRepository;
import com.elice.cinema.domain.screening.dto.response.ReservationScheduleResponse;
import com.elice.cinema.domain.screening.entity.Screening;
import com.elice.cinema.domain.screening.mapper.ScreeningMapper;
import com.elice.cinema.domain.screening.repository.ScreeningRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    private final MovieMapper movieMapper;
    private final ScreeningMapper screeningMapper;
    private final ReservationMapper reservationMapper;

    private final MovieImageRepository movieImageRepository;

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

    //TODO: 나중에 내 예매만 가능하게 조건 걸어주기
    public TossPaymentReservationResponse getTossPage(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        String orderId = "RESERVATION-" + reservation.getId();
        return reservationMapper.toPaymentReservationResponse(reservation, orderId, tossClientKey);
    }

    private Integer calculateRemainingSeats(Screening screening) {
        int totalSeats = screening.getScreen().getTotalSeats();
        int reservedCount = reservedSeatRepository.countAllByScreening_Id(screening.getId());

        return totalSeats - reservedCount;
    }
}
