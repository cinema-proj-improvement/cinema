package com.elice.cinema.domain.movieImage.service;

import com.elice.cinema.domain.movieImage.dto.MovieImageUploadResult;
import com.elice.cinema.global.common.file.FileCategory;
import com.elice.cinema.global.common.file.FileService;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieImageFileService {

    private final FileService fileService;

    public MovieImageUploadResult uploadAll(MultipartFile thumbnail, List<MultipartFile> extras) {
        if (thumbnail == null || thumbnail.isEmpty()) {
            throw new BusinessException(ErrorCode.MOVIE_THUMBNAIL_REQUIRED);
        }
        return upload(thumbnail, extras);
    }

    /** 수정 API용: 썸네일은 선택 사항(안 바뀌면 null/empty로 넘어옴) — 필수 검증 없이 업로드. */
    public MovieImageUploadResult uploadChanged(MultipartFile thumbnail, List<MultipartFile> extras) {
        return upload(thumbnail, extras);
    }

    private MovieImageUploadResult upload(MultipartFile thumbnail, List<MultipartFile> extras) {
        List<String> uploadedSoFar = new ArrayList<>();

        try {
            String thumbnailKey = null;
            if (thumbnail != null && !thumbnail.isEmpty()) {
                thumbnailKey = fileService.upload(thumbnail, FileCategory.MOVIE_THUMBNAIL);
                uploadedSoFar.add(thumbnailKey);
            }

            List<String> extraKeys = new ArrayList<>();
            if (extras != null) {
                for (MultipartFile extra : extras) {
                    if (extra == null || extra.isEmpty()) continue;
                    String key = fileService.upload(extra, FileCategory.MOVIE_EXTRA);
                    uploadedSoFar.add(key);
                    extraKeys.add(key);
                }
            }

            log.info("영화 이미지 업로드 완료: thumbnailUploaded={}, extraCount={}", thumbnailKey != null, extraKeys.size());
            return new MovieImageUploadResult(thumbnailKey, extraKeys);

        } catch (Exception e) {
            deleteAllQuietly(uploadedSoFar);
            throw e;
        }
    }

    public void deleteAllQuietly(List<String> keys) {
        for (String key : keys) {
            try {
                fileService.delete(key);
            } catch (Exception e) {
                log.error("파일 보상 삭제 실패 (무시됨): key={}", key, e);
            }
        }
    }
}
