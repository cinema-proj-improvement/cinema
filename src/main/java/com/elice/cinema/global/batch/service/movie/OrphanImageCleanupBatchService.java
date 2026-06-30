package com.elice.cinema.global.batch.service.movie;

import com.elice.cinema.domain.movieImage.repository.MovieImageRepository;
import com.elice.cinema.global.common.file.FileCategory;
import com.elice.cinema.global.common.file.FileMetadata;
import com.elice.cinema.global.common.file.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrphanImageCleanupBatchService {

    private final FileService fileService;
    private final MovieImageRepository movieImageRepository;

    @Transactional(readOnly = true)
    public void cleanup() {
        Instant cutoff = Instant.now().minus(10, ChronoUnit.MINUTES);
        Set<String> dbKeys = new HashSet<>(movieImageRepository.findAllImageKeys());

        int deleted = 0;
        int failed = 0;

        for (FileCategory category : List.of(FileCategory.MOVIE_THUMBNAIL, FileCategory.MOVIE_EXTRA)) {
            List<FileMetadata> storedFiles = fileService.listFiles(category);
            for (FileMetadata file : storedFiles) {
                if (file.uploadedAt().isAfter(cutoff)) {
                    continue; // 최근 10분 이내 파일은 건드리지 않음
                }
                if (dbKeys.contains(file.key())) {
                    continue;
                }
                try {
                    fileService.delete(file.key());
                    deleted++;
                } catch (Exception e) {
                    log.warn("고아 이미지 파일 삭제 실패: key={}", file.key(), e);
                    failed++;
                }
            }
        }

        log.info("고아 이미지 정리 완료: deleted={}, failed={}", deleted, failed);
    }
}
