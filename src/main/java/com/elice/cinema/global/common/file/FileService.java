package com.elice.cinema.global.common.file;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {
    /** 파일을 저장하고 스토리지 내 key를 반환한다. (예: movies/thumbnails/uuid.jpg) */
    String upload(MultipartFile file, FileCategory category);

    /** key에 해당하는 파일을 삭제한다. best-effort — 실패해도 예외를 던지지 않는다. */
    void delete(String key);

    /** 카테고리 하위의 모든 파일 목록을 반환한다. 고아 파일 정리 배치용. */
    List<FileMetadata> listFiles(FileCategory category);

    /** 스토리지 이미지 URL prefix를 반환한다. (local: /uploads, CDN: https://xxx.cloudfront.net) */
    String getImageBaseUrl();

    /** key → 클라이언트에 내릴 전체 이미지 URL */
    default String toImageUrl(String key) {
        if (key == null) return null;
        return getImageBaseUrl() + "/" + key;
    }
}
