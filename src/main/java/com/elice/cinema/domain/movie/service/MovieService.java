package com.elice.cinema.domain.movie.service;

import com.elice.cinema.domain.movie.dto.request.AdminMovieSearchRequest;
import com.elice.cinema.domain.movie.dto.response.AdminMovieListResponse;
import com.elice.cinema.domain.movie.dto.response.MovieUpdateFormResponse;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.mapper.MovieMapper;
import com.elice.cinema.domain.movie.repository.MovieRepository;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;

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

    /*                  공통 로직                   */

    private Movie findMovieById(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND));
    }

}
