package com.elice.cinema.global.common.file.local;

import com.elice.cinema.global.common.file.FileCategory;
import com.elice.cinema.global.common.file.FileService;
import com.elice.cinema.global.config.properties.FileProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "file.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalFileService implements FileService {
    private final FileProperties fileProperties;

    @Override
    public String upload(MultipartFile file, FileCategory category) {
        if(file == null || file.isEmpty()) {
            return null;
        }

        try {
            String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String savedFileName = UUID.randomUUID() + "." + extension;  // 원본 파일명은 필요로 하는 요구사항이 없기 때문에 생략

            File dir = new File(fileProperties.getUpload().getBasePath(), category.getDir());

            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("디렉토리 생성 실패: " + dir.getAbsolutePath());
            }

            File target = new File(dir, savedFileName);
            file.transferTo(target);

            return fileProperties.getUpload().getUrlPrefix()
                    + "/" + category.getDir()
                    + "/" + savedFileName;

        } catch (IOException e) {
            log.error("로컬 파일 업로드 실패", e);
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);  // TODO: FileUploadException 또는 BusinessException(ErrorCode.FILE_UPLOAD_FAILED)로 교체
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return;
        }

        try {
            String urlPrefix = fileProperties.getUpload().getUrlPrefix();

            // /uploads/xxx/yyy.png -> xxx/yyy.png
            if (!fileUrl.startsWith(urlPrefix)) {
                log.warn("삭제 대상 파일이 url-prefix와 일치하지 않음: {}", fileUrl);
                return;
            }

            String relativePath = fileUrl.substring(urlPrefix.length());
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }

            File target = new File(
                    fileProperties.getUpload().getBasePath(),
                    relativePath
            );

            if (!target.exists()) {
                log.warn("삭제할 파일이 존재하지 않음: {}", target.getAbsolutePath());
                return;
            }

            if (!target.delete()) {
                log.warn("파일 삭제 실패(권한 문제 가능): {}", target.getAbsolutePath());
            }

        } catch (Exception e) {
            log.error("로컬 파일 삭제 중 오류 발생: {}", fileUrl, e);
        }
    }
}
