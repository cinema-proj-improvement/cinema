package com.elice.cinema.domain.movie.facade;

import com.elice.cinema.domain.movie.dto.request.MovieCreateRequest;
import com.elice.cinema.domain.movie.service.MovieService;
import com.elice.cinema.domain.movieImage.dto.MovieImageUploadResult;
import com.elice.cinema.domain.movieImage.service.MovieImageFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MovieRegistrationFacade {

    private final MovieImageFileService movieImageFileService;
    private final MovieService movieService;

    public Long createMovie(MovieCreateRequest req) {
        MovieImageUploadResult uploadResult = movieImageFileService.uploadAll(
                req.getThumbnailImage(), req.getExtraImages()
        );

        try {
            Long movieId = movieService.saveMovieWithImages(req, uploadResult);
            log.info("영화 등록 완료: movieId={}", movieId);
            return movieId;
        } catch (Exception e) {
            log.warn("영화 DB 저장 실패, 업로드된 이미지 파일 보상 삭제 시작: uploadedKeys={}", uploadResult.getAllKeys());
            movieImageFileService.deleteAllQuietly(uploadResult.getAllKeys());
            throw e;
        }
    }
}
