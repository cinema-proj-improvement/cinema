package com.elice.cinema.global.common.file.local;

import com.elice.cinema.global.common.file.FileCategory;
import com.elice.cinema.global.common.file.FileMetadata;
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
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "file.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalFileService implements FileService {

    private final FileProperties fileProperties;

    @Override
    public String upload(MultipartFile file, FileCategory category) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + "." + extension;
            String key = category.getDir() + "/" + fileName;

            File dir = new File(fileProperties.getUpload().getBasePath(), category.getDir());
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("디렉토리 생성 실패: " + dir.getAbsolutePath());
            }

            file.transferTo(new File(dir, fileName));
            return key;

        } catch (IOException e) {
            log.error("로컬 파일 업로드 실패", e);
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * best-effort 삭제 — 보상 처리 경로에서 호출되므로 예외를 던지지 않는다.
     */
    @Override
    public void delete(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }

        File target = new File(fileProperties.getUpload().getBasePath(), key);
        if (!target.exists()) {
            log.warn("삭제할 파일이 존재하지 않음: {}", target.getAbsolutePath());
            return;
        }

        if (!target.delete()) {
            log.warn("파일 삭제 실패(권한 문제 가능): {}", target.getAbsolutePath());
        }
    }

    @Override
    public List<FileMetadata> listFiles(FileCategory category) {
        File dir = new File(fileProperties.getUpload().getBasePath(), category.getDir());
        if (!dir.exists()) return List.of();

        File[] files = dir.listFiles(File::isFile);
        if (files == null) return List.of();

        return Arrays.stream(files)
                .map(f -> new FileMetadata(
                        category.getDir() + "/" + f.getName(),
                        Instant.ofEpochMilli(f.lastModified())
                ))
                .toList();
    }

    @Override
    public String getImageBaseUrl() {
        return fileProperties.getStorage().getImageBaseUrl();
    }
}
