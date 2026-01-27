package com.elice.cinema.domain.movieImage.service;

import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.repository.MovieRepository;
import com.elice.cinema.domain.movieImage.entity.MovieImage;
import com.elice.cinema.domain.movieImage.repository.MovieImageRepository;
import com.elice.cinema.global.common.file.FileCategory;
import com.elice.cinema.global.common.file.FileService;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieImageService {
    private final MovieRepository movieRepository;
    private final MovieImageRepository movieImageRepository;
    private final FileService fileService;

    public void storeImages(Long movieId, MultipartFile thumbnailImage, List<MultipartFile> extraImages) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND));

        // 1) 썸네일 필수
        if (thumbnailImage == null || thumbnailImage.isEmpty()) {
            throw new BusinessException(ErrorCode.MOVIE_THUMBNAIL_REQUIRED);
        }

        // (선택) 이미 썸네일이 존재하는 movieId면 방어
        // movieImageRepository.findByMovieIdAndDisplayOrder(movieId, 0)
        //        .ifPresent(x -> { throw new BusinessException(ErrorCode.MOVIE_THUMBNAIL_ALREADY_EXISTS); });

        String thumbnailUrl = fileService.upload(thumbnailImage, FileCategory.MOVIE_THUMBNAIL);
        movieImageRepository.save(MovieImage.thumbnail(movie, thumbnailUrl));

        // 2) 추가 이미지: 1..n
        if (extraImages == null || extraImages.isEmpty()) {
            return;
        }

        int order = 1;
        for (MultipartFile f : extraImages) {
            if (f == null || f.isEmpty()) continue;

            String url = fileService.upload(f, FileCategory.MOVIE_EXTRA);
            movieImageRepository.save(MovieImage.extra(movie, url, order++));
        }
    }
}
