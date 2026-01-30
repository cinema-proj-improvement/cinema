package com.elice.cinema.global.batch.service.movie;

import com.elice.cinema.domain.movie.entity.MovieStatus;
import com.elice.cinema.domain.movie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class MovieStatusBatchService {

    private final MovieRepository movieRepository;

    @Transactional
    public void updateStatusesAtMidnight() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 1) 개봉일 도래: UPCOMING -> NOW_SHOWING
        int toNowShowing = movieRepository.bulkUpdateUpcomingToNowShowing(MovieStatus.UPCOMING, MovieStatus.NOW_SHOWING, today);

        // 2) 종료일 도래: (정책에 맞게) -> ENDED
        // 같은 날 releaseDate == endDate 인 케이스를 고려해서 ENDED를 나중에 실행
        int toEnded = movieRepository.bulkUpdateToEndedByEndDate(MovieStatus.ENDED, today);

        // 필요하면 로깅
        // log.info("[MovieStatusBatch] today={}, toNowShowing={}, toEnded={}", today, toNowShowing, toEnded);
    }
}