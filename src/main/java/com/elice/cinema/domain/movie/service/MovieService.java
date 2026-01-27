package com.elice.cinema.domain.movie.service;

import com.elice.cinema.domain.movie.dto.request.MovieCreateRequest;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.event.MovieImagesStorageEvent;
import com.elice.cinema.domain.movie.mapper.MovieMapper;
import com.elice.cinema.domain.movie.repository.MovieRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MovieService {
    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final ApplicationEventPublisher publisher;

    // 관리자 - 영화 생성 요청을 받아 영화를 생성하고 DB에 저장하는 메서드
    @Transactional
    public Long createMovie(MovieCreateRequest req) {
        validateDates(req.getReleaseDate(), req.getEndDate());

        Movie movie = movieMapper.toEntity(req);
        movieRepository.save(movie);

        // TODO: transactional phase after-commit으로 설정해야 영화 등록 실패해서 rollback 되고 이미지 파일만 등록되는 것 방지 가능?
        // TODO: 근데 위에처럼 처리하면 영화 생성될 때 thumbnailImageUrl 필드 못 넣어주지 않나? 이벤트 핸들러에서 처리..?
        publisher.publishEvent(MovieImagesStorageEvent.of(
                movie.getId(),
                req.getThumbnailImage(),
                req.getExtraImages()
        ));

        return movie.getId();
    }



    // === Helper Methods ===
    private void validateDates(LocalDate releaseDate, LocalDate endDate) {  // FIXME: 이 로직을 DTO level에서 custom annotation으로?
        if(!endDate.isAfter(releaseDate)) {  // 개봉일과 종료일이 동일한 케이스도 에러로 취급
            throw new BusinessException(ErrorCode.MOVIE_INVALID_DATE_RANGE);
        }
    }
}