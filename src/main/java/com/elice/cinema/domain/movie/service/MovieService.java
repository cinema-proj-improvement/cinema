package com.elice.cinema.domain.movie.service;

import com.elice.cinema.domain.movie.dto.request.MovieCreateRequest;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.mapper.MovieMapper;
import com.elice.cinema.domain.movie.repository.MovieRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MovieService {
    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;

    // 관리자 - 영화 생성 요청을 받아 영화를 생성하고 DB에 저장하는 메서드
    public Long createMovie(MovieCreateRequest req) {
        validateDates(req.getReleaseDate(), req.getEndDate());
        Movie movie = movieMapper.toEntity(req);
        movieRepository.save(movie);
        return movie.getId();
    }



    // === Helper Methods ===
    private void validateDates(LocalDate releaseDate, LocalDate endDate) {
        if(!endDate.isAfter(releaseDate)) {  // 개봉일과 종료일이 동일한 케이스도 에러로 취급
            throw new BusinessException(ErrorCode.MOVIE_INVALID_DATE_RANGE);
        }
    }
}