package com.elice.cinema.global.common.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    /**
     * 파일 업로드
     * @param file 업로드할 파일
     * @return 저장된 파일의 접근 URL (DB 저장용)
     */
    String upload(MultipartFile file, FileCategory category);

    /**
     * 파일 삭제
     * @param fileUrl DB에 저장된 파일 URL
     */
    void delete(String fileUrl);
}
