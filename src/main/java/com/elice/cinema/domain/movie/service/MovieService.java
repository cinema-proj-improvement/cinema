package com.elice.cinema.domain.movie.service;

import com.elice.cinema.domain.movie.dto.request.AdminMovieSearchRequest;
import com.elice.cinema.domain.movie.dto.request.MovieCreateRequest;
import com.elice.cinema.domain.movie.dto.response.AdminMovieListResponse;
import com.elice.cinema.domain.movie.dto.response.MovieUpdateFormResponse;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.event.MovieImagesStorageEvent;
import com.elice.cinema.domain.movie.mapper.MovieMapper;
import com.elice.cinema.domain.movie.repository.MovieRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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

        publisher.publishEvent(MovieImagesStorageEvent.of(
                movie.getId(),
                req.getThumbnailImage(),
                req.getExtraImages()
        ));

        return movie.getId();
    }

    // 관리자 영화 목록 조회 (검색조건 + 페이지네이션 + 정렬)
    public Page<AdminMovieListResponse> getAdminMovieListPage(AdminMovieSearchRequest request, Pageable pageable) {
        return movieRepository.findAdminMovieList(request, pageable)
                .map(movieMapper::toAdminListResponse);
    }

    // 관리자 상세 조회
    public AdminMovieListResponse getAdminMovieDetail(Long movieId) {
        Movie movie = findMovieById(movieId);
        return movieMapper.toAdminListResponse(movie);
    }

    // 업데이트 폼 조회
    public MovieUpdateFormResponse getMovieUpdateForm(Long movieId) {
        Movie movie = findMovieById(movieId);
        return movieMapper.toMovieUpdateFormResponse(movie);
    }



    // === Helper Methods ===
    private void validateDates(LocalDate releaseDate, LocalDate endDate) {  // FIXME: 이 로직을 DTO level에서 custom annotation으로?
        if(!endDate.isAfter(releaseDate)) {  // 개봉일과 종료일이 동일한 케이스도 에러로 취급
            throw new BusinessException(ErrorCode.MOVIE_INVALID_DATE_RANGE);
        }
    }

    private Movie findMovieById(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND));
    }
}