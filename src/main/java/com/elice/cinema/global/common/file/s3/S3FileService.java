package com.elice.cinema.global.common.file.s3;

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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "file.storage", name = "type", havingValue = "s3")
public class S3FileService implements FileService {

    private final S3Client s3Client;
    private final FileProperties fileProperties;

    @Override
    public String upload(MultipartFile file, FileCategory category) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String bucket = fileProperties.getStorage().getS3Bucket();
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String key = category.getDir() + "/" + UUID.randomUUID() + "." + extension;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException | S3Exception e) {
            log.error("S3 파일 업로드 실패: key={}", key, e);
            throw new RuntimeException("S3 파일 업로드 실패: " + key, e);
        }

        return key;
    }

    /**
     * best-effort 삭제 — 보상 처리 경로에서 호출되므로 예외를 던지지 않는다.
     */
    @Override
    public void delete(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(fileProperties.getStorage().getS3Bucket())
                    .key(key)
                    .build());
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패 (보상 처리 중 무시됨): key={}", key, e);
        }
    }

    @Override
    public List<FileMetadata> listFiles(FileCategory category) {
        String bucket = fileProperties.getStorage().getS3Bucket();
        String prefix = category.getDir() + "/";

        List<FileMetadata> result = new ArrayList<>();
        ListObjectsV2Iterable paginator = s3Client.listObjectsV2Paginator(
                ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(prefix)
                        .build()
        );

        for (S3Object obj : paginator.contents()) {
            result.add(new FileMetadata(obj.key(), obj.lastModified()));
        }

        return result;
    }

    @Override
    public String getImageBaseUrl() {
        return fileProperties.getStorage().getImageBaseUrl();
    }
}
