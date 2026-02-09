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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieImageService {
    private final MovieRepository movieRepository;
    private final MovieImageRepository movieImageRepository;
    private final FileService fileService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void storeImages(Long movieId, MultipartFile thumbnailImage, List<MultipartFile> extraImages) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND));

        // 1) 썸네일 필수
        if (thumbnailImage == null || thumbnailImage.isEmpty()) {
            throw new BusinessException(ErrorCode.MOVIE_THUMBNAIL_REQUIRED);
        }

        String thumbnailUrl = fileService.upload(thumbnailImage, FileCategory.MOVIE_THUMBNAIL);  // FIXME: 파일 처리가 Transaction 안에 묶여있음. 분리 필요
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

    /**
     * 부분 교체(A-변형)
     * - 썸네일만 변경: 썸네일만 DB/파일 삭제 후 재저장
     * - extra만 변경: extra만 DB/파일 삭제 후 재저장
     * - 둘 다 변경: 전체 삭제 후 재저장
     *
     * 파일 삭제는 AFTER_COMMIT, 롤백이면 새 파일 정리(AFTER_ROLLBACK).
     */
    @Transactional
    public void updateImages(Long movieId, MultipartFile newThumbnail, List<MultipartFile> newExtraImages) {

        boolean thumbnailChanged = hasFile(newThumbnail);
        boolean extrasChanged = hasAnyFile(newExtraImages);

        if (!thumbnailChanged && !extrasChanged) {
            return; // 이미지 변경 없음
        }

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND));

        List<String> deleteAfterCommit = new ArrayList<>();
        List<String> cleanupOnRollback = new ArrayList<>();

        // 1) 기존 이미지 조회 + DB 삭제 범위 결정
        if (thumbnailChanged && extrasChanged) {
            // 전체 삭제
            movieImageRepository.findByMovieIdOrderByDisplayOrderAsc(movieId)
                    .forEach(mi -> deleteAfterCommit.add(mi.getImageUrl()));
            movieImageRepository.deleteAllByMovieId(movieId);

        } else if (thumbnailChanged) {
            // 썸네일만 삭제
            movieImageRepository.findByMovieIdAndDisplayOrder(movieId, 0)
                    .ifPresent(mi -> deleteAfterCommit.add(mi.getImageUrl()));
            movieImageRepository.deleteThumbnailByMovieId(movieId);

        } else {
            // extra만 삭제
            movieImageRepository.findByMovieIdAndDisplayOrderGreaterThanEqualOrderByDisplayOrderAsc(movieId, 1)
                    .forEach(mi -> deleteAfterCommit.add(mi.getImageUrl()));
            movieImageRepository.deleteExtrasByMovieId(movieId);
        }

        // 2) 새 파일 업로드 + DB 저장
        if (thumbnailChanged) {
            String thumbUrl = fileService.upload(newThumbnail, FileCategory.MOVIE_THUMBNAIL);
            if (thumbUrl == null) throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
            cleanupOnRollback.add(thumbUrl);
            movieImageRepository.save(MovieImage.thumbnail(movie, thumbUrl));
        }

        if (extrasChanged) {
            int order = 1;
            for (MultipartFile f : newExtraImages) {
                if (!hasFile(f)) continue;
                String url = fileService.upload(f, FileCategory.MOVIE_EXTRA);
                if (url == null) continue;
                cleanupOnRollback.add(url);
                movieImageRepository.save(MovieImage.extra(movie, url, order++));
            }
        }

        // 3) 트랜잭션 훅 등록 (파일 정리)
        registerCleanupHooks(deleteAfterCommit, cleanupOnRollback);
    }



    // === Helper methods ===

    private void registerCleanupHooks(List<String> deleteAfterCommit, List<String> cleanupOnRollback) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("updateImages는 트랜잭션 내부에서 호출되어야 합니다.");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (String url : deleteAfterCommit) {
                    fileService.delete(url);
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    for (String url : cleanupOnRollback) {
                        fileService.delete(url);
                    }
                }
            }
        });
    }

    private boolean hasFile(MultipartFile f) {
        return f != null && !f.isEmpty();
    }

    private boolean hasAnyFile(List<MultipartFile> files) {
        if (files == null) return false;
        for (MultipartFile f : files) {
            if (hasFile(f)) return true;
        }
        return false;
    }
}
