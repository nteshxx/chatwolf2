package com.chatwolf.storage.service;

import com.chatwolf.storage.entity.FileMetadata;
import com.chatwolf.storage.repository.FileMetadataRepository;
import io.minio.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaProcessingService {

    private final MinioClient minioClient;
    private final FileMetadataRepository metadataRepository;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.thumbnail-bucket}")
    private String thumbnailBucket;

    @Async("storageTaskExecutor")
    public CompletableFuture<Void> processImageAsync(String objectKey) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Processing image: {}", objectKey);

                FileMetadata metadata = metadataRepository
                        .findByObjectKey(objectKey)
                        .orElseThrow(() -> new RuntimeException("Metadata not found"));

                metadata.setStatus("PROCESSING");
                metadataRepository.save(metadata);

                // Download image
                try (InputStream is = minioClient.getObject(
                        GetObjectArgs.builder().bucket(bucket).object(objectKey).build())) {

                    BufferedImage image = ImageIO.read(is);

                    if (image != null) {
                        // Extract dimensions
                        metadata.setWidth(image.getWidth());
                        metadata.setHeight(image.getHeight());

                        // Generate thumbnail
                        String thumbnailKey = generateThumbnailKey(objectKey);
                        createThumbnail(image, thumbnailKey);
                        metadata.setThumbnailKey(thumbnailKey);
                    }
                }

                metadata.setStatus("READY");
                metadataRepository.save(metadata);

                log.info("Image processing completed: {}", objectKey);

            } catch (Exception e) {
                log.error("Image processing failed: {}", objectKey, e);
                updateStatusToFailed(objectKey);
            }
        });
    }

    @Async("storageTaskExecutor")
    public CompletableFuture<Void> processVideoAsync(String objectKey) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Processing video: {}", objectKey);

                FileMetadata metadata = metadataRepository
                        .findByObjectKey(objectKey)
                        .orElseThrow(() -> new RuntimeException("Metadata not found"));

                metadata.setStatus("PROCESSING");
                metadataRepository.save(metadata);

                // For video processing, you'd typically use FFmpeg
                // This is a simplified example

                // Generate video thumbnail (first frame)
                String thumbnailKey = generateThumbnailKey(objectKey);
                // createVideoThumbnail(objectKey, thumbnailKey);
                metadata.setThumbnailKey(thumbnailKey);

                // Extract video metadata
                // VideoMetadata videoMeta = extractVideoMetadata(objectKey);
                // metadata.setWidth(videoMeta.getWidth());
                // metadata.setHeight(videoMeta.getHeight());
                // metadata.setDuration(videoMeta.getDuration());

                metadata.setStatus("READY");
                metadataRepository.save(metadata);

                log.info("Video processing completed: {}", objectKey);

            } catch (Exception e) {
                log.error("Video processing failed: {}", objectKey, e);
                updateStatusToFailed(objectKey);
            }
        });
    }

    private void createThumbnail(BufferedImage original, String thumbnailKey) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Thumbnails.of(original)
                .size(300, 300)
                .outputFormat("jpg")
                .outputQuality(0.8)
                .toOutputStream(baos);

        byte[] thumbnailBytes = baos.toByteArray();

        try (ByteArrayInputStream bais = new ByteArrayInputStream(thumbnailBytes)) {
            minioClient.putObject(PutObjectArgs.builder().bucket(thumbnailBucket).object(thumbnailKey).stream(
                            bais, thumbnailBytes.length, -1)
                    .contentType("image/jpeg")
                    .build());
        }
    }

    private String generateThumbnailKey(String objectKey) {
        return "thumb-" + objectKey.replace("/", "-");
    }

    private void updateStatusToFailed(String objectKey) {
        try {
            FileMetadata metadata =
                    metadataRepository.findByObjectKey(objectKey).orElse(null);
            if (metadata != null) {
                metadata.setStatus("FAILED");
                metadataRepository.save(metadata);
            }
        } catch (Exception e) {
            log.error("Failed to update status for: {}", objectKey, e);
        }
    }
}
