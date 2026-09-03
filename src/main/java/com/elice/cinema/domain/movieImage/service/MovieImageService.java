package com.elice.cinema.domain.movieImage.service;

import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movieImage.dto.MovieImageUploadResult;
import com.elice.cinema.domain.movieImage.entity.MovieImage;
import com.elice.cinema.domain.movieImage.repository.MovieImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieImageService {
    private final MovieImageRepository movieImageRepository;

    /**
     * 이미 업로드된 이미지 key로 기존 row를 교체한다 (DB 전용, 파일 I/O 없음).
     * 교체된(=삭제된) 기존 key 목록을 반환한다 — 실제 파일 삭제는 호출자 책임.
     */
    @Transactional
    public List<String> replaceImages(Movie movie, MovieImageUploadResult uploadResult) {
        List<String> oldKeys = new ArrayList<>();

        if (uploadResult.thumbnailKey() != null) {
            movieImageRepository.findByMovieIdAndDisplayOrder(movie.getId(), 0)
                    .ifPresent(mi -> oldKeys.add(mi.getImageUrl()));
            movieImageRepository.deleteThumbnailByMovieId(movie.getId());
            movieImageRepository.save(MovieImage.thumbnail(movie, uploadResult.thumbnailKey()));
        }

        if (!uploadResult.extraKeys().isEmpty()) {
            movieImageRepository.findByMovieIdAndDisplayOrderGreaterThanEqualOrderByDisplayOrderAsc(movie.getId(), 1)
                    .forEach(mi -> oldKeys.add(mi.getImageUrl()));
            movieImageRepository.deleteExtrasByMovieId(movie.getId());

            int order = 1;
            for (String key : uploadResult.extraKeys()) {
                movieImageRepository.save(MovieImage.extra(movie, key, order++));
            }
        }

        return oldKeys;
    }
}
