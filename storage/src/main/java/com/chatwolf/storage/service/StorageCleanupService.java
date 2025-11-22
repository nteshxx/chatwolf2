package com.chatwolf.storage.service;

import com.chatwolf.storage.entity.FileMetadata;
import com.chatwolf.storage.repository.FileMetadataRepository;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageCleanupService {

    private final MinioClient minioClient;
    private final FileMetadataRepository metadataRepository;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${storage.retention-days:90}")
    private int retentionDays;

    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
    @Transactional
    public void cleanupOldFiles() {
        log.info("Starting cleanup of old files");

        Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        List<FileMetadata> oldFiles = metadataRepository.findOldFiles(cutoffDate);

        int deletedCount = 0;
        for (FileMetadata file : oldFiles) {
            try {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(file.getObjectKey())
                        .build());

                metadataRepository.delete(file);
                deletedCount++;

            } catch (Exception e) {
                log.error("Failed to delete old file: {}", file.getObjectKey(), e);
            }
        }

        log.info("Cleanup completed. Deleted {} old files", deletedCount);
    }

    @Scheduled(cron = "0 30 2 * * *") // Run at 2:30 AM daily
    @Transactional
    public void cleanupOrphanedChunks() {
        log.info("Starting cleanup of orphaned chunks");
        // Implementation for cleaning up incomplete chunk uploads
    }
}
