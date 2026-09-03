package com.elice.cinema.domain.movie.facade;

import com.elice.cinema.domain.movie.dto.request.MovieUpdateRequest;
import com.elice.cinema.domain.movie.service.MovieService;
import com.elice.cinema.domain.movieImage.dto.MovieImageUploadResult;
import com.elice.cinema.domain.movieImage.service.MovieImageFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MovieUpdateFacade {

    private final MovieImageFileService movieImageFileService;
    private final MovieService movieService;

    public void updateMovie(Long movieId, MovieUpdateRequest req) {
        if (!req.hasAnyImageChange()) {
            movieService.updateMovie(movieId, req);
            return;
        }

        MovieImageUploadResult uploadResult = movieImageFileService.uploadChanged(
                req.getThumbnailImage(), req.getExtraImages()
        );

        List<String> oldKeys;
        try {
            oldKeys = movieService.updateMovieWithImages(movieId, req, uploadResult);
        } catch (Exception e) {
            log.warn("영화 수정 DB 저장 실패, 업로드된 새 이미지 파일 보상 삭제 시작: uploadedKeys={}", uploadResult.getAllKeys());
            movieImageFileService.deleteAllQuietly(uploadResult.getAllKeys());
            throw e;
        }

        log.info("영화 수정 완료: movieId={}, 기존 이미지 파일 삭제 시작: oldKeys={}", movieId, oldKeys);
        movieImageFileService.deleteAllQuietly(oldKeys);
    }
}
