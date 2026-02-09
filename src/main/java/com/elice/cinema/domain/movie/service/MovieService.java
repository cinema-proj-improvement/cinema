package com.elice.cinema.domain.movie.service;

import com.elice.cinema.domain.movie.dto.request.AdminMovieSearchRequest;
import com.elice.cinema.domain.movie.dto.request.AdminMovieSortType;
import com.elice.cinema.domain.movie.dto.request.MovieCreateRequest;
import com.elice.cinema.domain.movie.dto.request.MovieUpdateRequest;
import com.elice.cinema.domain.movie.dto.response.*;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.entity.MovieStatus;
import com.elice.cinema.domain.movie.event.MovieImagesStorageEvent;
import com.elice.cinema.domain.movie.mapper.MovieMapper;
import com.elice.cinema.domain.movie.repository.AdminMovieJoinQueryRepository;
import com.elice.cinema.domain.movie.repository.MovieRepository;
import com.elice.cinema.domain.movieImage.entity.MovieImage;
import com.elice.cinema.domain.movieImage.repository.MovieImageRepository;
import com.elice.cinema.domain.movieImage.service.MovieImageService;
import com.elice.cinema.domain.screening.service.ScreeningService;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieService {
    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final ApplicationEventPublisher publisher;
    private final MovieImageRepository movieImageRepository;
    private final AdminMovieJoinQueryRepository adminMovieJoinQueryRepository;

    private final MovieImageService movieImageService;
    private final ScreeningService screeningService;

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
    public Page<AdminMovieListResponse> getAdminMovieListPage(
            AdminMovieSearchRequest request,
            Pageable pageable
    ) {
        if (request.getSortType() == AdminMovieSortType.AVG_SCORE_DESC) {

            throw new BusinessException(ErrorCode.MOVIE_SORT_NOT_SUPPORTED);
        }
        List<Long> movieIds =
                movieRepository.findAdminMovieIds(request, pageable);

        if (movieIds.isEmpty()) {
            return Page.empty(pageable);
        }

        long totalCount =
                movieRepository.countAdminMovies(request);

        List<AdminMovieJoinRowResponse> rows =
                adminMovieJoinQueryRepository.findAdminMovieJoinRows(
                        movieIds,
                        request.getSortType()
                );

        List<AdminMovieListResponse> contents =
                AdminMovieListResponse.fromRows(rows);

        return new PageImpl<>(contents, pageable, totalCount);
    }

    // 관리자 상세 조회
    public MovieDetailResponse getAdminMovieDetail(Long movieId) {

        Movie movie = findMovieById(movieId);

        String thumbnail = movieImageRepository
                .findThumbnailUrlByMovieId(movieId)
                .orElse(null);

        List<String> images = movieImageRepository
                .findExtraImagesByMovieId(movieId);

        return movieMapper.toMovieDetailResponse(movie, thumbnail, images);
    }

    // 업데이트 폼 조회
    public MovieUpdateFormResponse getMovieUpdateForm(Long movieId) {
        Movie movie = findMovieById(movieId);

        MovieUpdateFormResponse movieUpdateFormResponse = movieMapper.toMovieUpdateFormResponse(movie);

        // MovieImage 조회해서 썸네일/extra 계산
        List<MovieImage> images = movieImageRepository.findByMovieIdOrderByDisplayOrderAsc(movieId);

        String thumbnailUrl = images.stream()
                .filter(MovieImage::isThumbnail)
                .findFirst()
                .map(MovieImage::getImageUrl)
                .orElse(null);

        List<String> extraImages = images.stream()
                .filter(mi -> !mi.isThumbnail())
                .sorted(Comparator.comparing(MovieImage::getDisplayOrder))
                .map(MovieImage::getImageUrl)
                .toList();

        movieUpdateFormResponse.setThumbnailImageUrl(thumbnailUrl);
        movieUpdateFormResponse.setExtraImages(extraImages);

        return movieUpdateFormResponse;
    }

    @Transactional
    public void updateMovie(Long movieId, MovieUpdateRequest req) {
        Movie movie = findMovieById(movieId);

        if(!movie.getRunningTimeMinutes().equals(req.getRunningTimeMinutes())) {  // 러닝타임 변경 관련 business rule
            if(screeningService.existsScreeningByMovieId(movieId)) {
                throw new BusinessException(ErrorCode.MOVIE_RUNNING_TIME_CANNOT_CHANGE_WHEN_SCREENING_EXISTS);
            }
        }

        movie.changeBasicInfo(
                req.getTitle(),
                req.getRunningTimeMinutes(),
                req.getAgeRating(),
                req.getSynopsis()
        );

        movie.changeGenres(new HashSet<>(req.getGenres()));
        movie.changeScreeningTypes(new HashSet<>(req.getScreeningTypes()));

        if(req.hasAnyImageChange()) {
            movieImageService.updateImages(movieId, req.getThumbnailImage(), req.getExtraImages());
        }
    }



    // === Helper Methods ===
    private void validateDates(LocalDate releaseDate, LocalDate endDate) {  // FIXME: 이 로직을 DTO level에서 custom annotation으로?
        if (!endDate.isAfter(releaseDate)) {  // 개봉일과 종료일이 동일한 케이스도 에러로 취급
            throw new BusinessException(ErrorCode.MOVIE_INVALID_DATE_RANGE);
        }
    }

    private Movie findMovieById(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND));
    }

    // 사용자 영화 목록 조회
    public Page<MovieListResponse> getUserMovieList(
            String keyword,
            String sort,
            Pageable pageable
    ) {
        return movieRepository.findUserMovies(keyword, sort, pageable)
                .map(movieMapper::toMovieListResponse);
    }

    // 사용자 영화 상세 조회
    public MovieDetailResponse getUserMovieDetail(Long movieId) {

        Movie movie = movieRepository.findUserMovieById(movieId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.MOVIE_NOT_FOUND)
                );

        String thumbnail = movieImageRepository
                .findThumbnailUrlByMovieId(movieId)
                .orElse(null);

        List<String> images = movieImageRepository
                .findExtraImagesByMovieId(movieId);

        return movieMapper.toMovieDetailResponse(movie, thumbnail, images);
    }

    // “상영 종료가 아닌 영화” 조회
    public List<MovieSelectResponse> getAvailableMoviesForScreening() {
        return movieRepository.findAllByStatusNot(MovieStatus.ENDED)
                .stream()
                .map(movieMapper::toMovieSelectResponse)
                .toList();
    }
}