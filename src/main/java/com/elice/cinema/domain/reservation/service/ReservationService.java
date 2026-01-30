package com.elice.cinema.domain.reservation.service;

import com.elice.cinema.domain.movie.dto.response.ReservationMovieSelectResponse;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.mapper.MovieMapper;
import com.elice.cinema.domain.movie.repository.MovieRepository;
import com.elice.cinema.domain.reservation.repository.ReservedSeatRepository;
import com.elice.cinema.domain.screening.dto.response.ReservationScheduleResponse;
import com.elice.cinema.domain.screening.entity.Screening;
import com.elice.cinema.domain.screening.mapper.ScreeningMapper;
import com.elice.cinema.domain.screening.repository.ScreeningRepository;
import lombok.RequiredArgsConstructor;
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

    private final MovieRepository movieRepository;
    private final ScreeningRepository screeningRepository;
    private final ReservedSeatRepository reservedSeatRepository;
    private final MovieMapper movieMapper;
    private final ScreeningMapper screeningMapper;

    public List<ReservationMovieSelectResponse> getMoviesWithScreeningsWithin() {
        LocalDate today = LocalDate.now();

        LocalDateTime from = today.atStartOfDay();
        LocalDateTime toExclusive = today.plusDays(DAYS_RANGE_INCLUSIVE + 1).atStartOfDay();

        //TODO: 현재는 7일이내 상영 상태가 OPEN인 상영이 있는 영화만 가져오는 중, 기능 명세에는 영화 개봉일 기준으로 가져오기로 함. 내 생각에는 상영이 있는 영화만 가져와서 보여주는게 좋아보임. 팀원 생각 물어보기
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

    private Integer calculateRemainingSeats(Screening screening) {
        int totalSeats = screening.getScreen().getTotalSeats();
        int reservedCount = reservedSeatRepository.countAllByScreening_Id(screening.getId());

        return totalSeats - reservedCount;
    }
}
