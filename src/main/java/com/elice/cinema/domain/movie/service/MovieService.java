package com.elice.cinema.domain.movie.service;

import com.elice.cinema.domain.movie.dto.request.AdminMovieSearchRequest;
import com.elice.cinema.domain.movie.dto.request.MovieCreateRequest;
import com.elice.cinema.domain.movie.dto.request.MovieUpdateRequest;
import com.elice.cinema.domain.movie.dto.response.AdminMovieListResponse;
import com.elice.cinema.domain.movie.dto.response.MovieUpdateFormResponse;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.event.MovieImagesStorageEvent;
import com.elice.cinema.domain.movie.mapper.MovieMapper;
import com.elice.cinema.domain.movie.repository.MovieRepository;
import com.elice.cinema.domain.movieImage.entity.MovieImage;
import com.elice.cinema.domain.movieImage.repository.MovieImageRepository;
import com.elice.cinema.domain.movieImage.service.MovieImageService;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
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
    private final MovieImageRepository movieImageRepository;
    private final MovieMapper movieMapper;
    private final ApplicationEventPublisher publisher;

    private final MovieImageService movieImageService;

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

    /* TODO:
        - 모든 영화정보는 언제든지 정보를 수정 가능하다.
            - 단, 러닝타임은 영화와 연관된 상영이 없을 때만 수정 가능하다.
            - 단, 상태는 Movie 내 releaseDate와 endDate를 기준으로 변경된다. (Batch Job을 통해 변경)
        - 썸네일 이미지 수정할 경우 MovieImages 테이블에 기존 썸네일 정보 삭제 후 새로 설정한 썸네일 넣어줘야 함.
            - 또한, 이미지 파일 저장 경로에 고아 파일 생기지 않도록 유의해서 설계 필요
        - 기타 이미지 수정은 어떻게..?
            - 기존에 있던 거 테이블에서 다 삭제 + 이미지 파일 저장 위치에서도 다 삭제 후 수정할 때 넣어준 이미지 파일들을 저장하는 방식?
            - 기존에 있던 거에서 뺀 게 있는지, 중복해서 추가한 게 있는지(애초에 기존에 있는 파일은 선택해서 못 넣도록?), 새로 추가한 게 있는지 확인해서
                테이블 수정 및 이미지 파일 경로에서 수정하는 방식?
            - 마찬가지 어느 방식이든 고아 파일 생기지 않도록 유의해서 설계 필요
     */
    @Transactional
    public void updateMovie(Long movieId, MovieUpdateRequest req) {
        Movie movie = findMovieById(movieId);

        // TODO: Screening 도메인 개발 후 반영하기
        // 러닝타임 변경 관련 business rule
//        if(!movie.getRunningTimeMinutes().equals(req.getRunningTimeMinutes())) {
//            if(screeningRepository.existsByMovieId(movieId)) {
//                throw new BusinessException(ErrorCode.MOVIE_RUNNING_TIME_CANNOT_CHANGE_WHEN_SCREENING_EXISTS);
//            }
//        }

        movie.changeBasicInfo(
                req.getTitle(),
                req.getRunningTimeMinutes(),
                req.getReleaseDate(),
                req.getEndDate(),
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
        if(!endDate.isAfter(releaseDate)) {  // 개봉일과 종료일이 동일한 케이스도 에러로 취급
            throw new BusinessException(ErrorCode.MOVIE_INVALID_DATE_RANGE);
        }
    }

    private Movie findMovieById(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND));
    }
}