package com.chatwolf.storage.service;

import com.chatwolf.storage.dto.ChunkUploadRequest;
import com.chatwolf.storage.dto.UploadResponse;
import com.chatwolf.storage.entity.FileMetadata;
import com.chatwolf.storage.exception.FileValidationException;
import com.chatwolf.storage.exception.QuotaExceededException;
import com.chatwolf.storage.exception.StorageException;
import com.chatwolf.storage.repository.FileMetadataRepository;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient minioClient;
    private final FileMetadataRepository metadataRepository;
    private final MediaProcessingService mediaProcessingService;
    private final StorageMetricsService metricsService;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.thumbnail-bucket}")
    private String thumbnailBucket;

    @Value("${upload.max-size:104857600}")
    private long maxFileSize;

    @Value("${upload.max-user-quota:10737418240}")
    private long maxUserQuota;

    @Value("${upload.chunk-size:5242880}")
    private int chunkSize;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/bmp", "image/svg+xml");

    private static final List<String> ALLOWED_VIDEO_TYPES =
            Arrays.asList("video/mp4", "video/webm", "video/quicktime", "video/x-msvideo", "video/mpeg");

    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    @Async("storageTaskExecutor")
    @Transactional
    public CompletableFuture<UploadResponse> uploadAsync(MultipartFile file, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            String objectKey = null;

            try {
                log.info(
                        "Starting upload for user: {}, filename: {}, size: {}",
                        userId,
                        file.getOriginalFilename(),
                        file.getSize());

                // Validation
                validateFile(file);
                checkUserQuota(userId, file.getSize());

                objectKey = generateObjectKey(file);
                String contentType = determineContentType(file);

                uploadToMinio(file, objectKey, contentType);

                FileMetadata metadata = createFileMetadata(file, objectKey, contentType, userId);
                metadataRepository.save(metadata);

                triggerMediaProcessing(objectKey, contentType);

                long duration = System.currentTimeMillis() - startTime;
                metricsService.recordUpload(file.getSize(), contentType, true, duration);

                log.info("Upload completed: {} in {}ms", objectKey, duration);

                return UploadResponse.builder()
                        .objectKey(objectKey)
                        .filename(file.getOriginalFilename())
                        .size(file.getSize())
                        .contentType(contentType)
                        .uploadedAt(Instant.now())
                        .success(true)
                        .build();

            } catch (Exception e) {
                log.error("Upload failed for user: {}", userId, e);
                metricsService.recordUpload(
                        file.getSize(), file.getContentType(), false, System.currentTimeMillis() - startTime);

                if (objectKey != null) {
                    cleanupFailedUpload(objectKey);
                }

                throw new StorageException("Upload failed: " + e.getMessage(), e);
            }
        });
    }

    @Async("storageTaskExecutor")
    @Transactional
    public CompletableFuture<UploadResponse> uploadChunkAsync(
            ChunkUploadRequest request, MultipartFile chunk, String userId) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                String chunkKey = generateChunkKey(request.getUploadId(), request.getChunkNumber());
                uploadToMinio(chunk, chunkKey, "application/octet-stream");

                if (request.getChunkNumber().equals(request.getTotalChunks())) {
                    return mergeChunks(
                            request.getUploadId(),
                            request.getTotalChunks(),
                            request.getFilename(),
                            request.getContentType(),
                            userId);
                }

                return UploadResponse.builder()
                        .uploadId(request.getUploadId())
                        .chunkNumber(request.getChunkNumber())
                        .totalChunks(request.getTotalChunks())
                        .complete(false)
                        .success(true)
                        .build();

            } catch (Exception e) {
                log.error(
                        "Chunk upload failed: uploadId={}, chunk={}",
                        request.getUploadId(),
                        request.getChunkNumber(),
                        e);
                throw new StorageException("Chunk upload failed", e);
            }
        });
    }

    public CompletableFuture<String> getPresignedUrlAsync(String objectKey, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                verifyFileAccess(objectKey, userId);

                String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket)
                        .object(objectKey)
                        .expiry(1, TimeUnit.HOURS)
                        .build());

                log.debug("Generated presigned URL for: {}", objectKey);
                return url;

            } catch (Exception e) {
                log.error("Failed to generate presigned URL for: {}", objectKey, e);
                throw new StorageException("Failed to generate presigned URL", e);
            }
        });
    }

    public CompletableFuture<InputStream> downloadAsync(String objectKey, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                verifyFileAccess(objectKey, userId);

                InputStream stream = minioClient.getObject(
                        GetObjectArgs.builder().bucket(bucket).object(objectKey).build());

                metricsService.recordDownload(objectKey, true);
                return stream;

            } catch (Exception e) {
                log.error("Download failed for: {}", objectKey, e);
                metricsService.recordDownload(objectKey, false);
                throw new StorageException("Download failed", e);
            }
        });
    }

    @Async("storageTaskExecutor")
    @Transactional
    public CompletableFuture<Void> deleteAsync(String objectKey, String userId) {
        return CompletableFuture.runAsync(() -> {
            try {
                FileMetadata metadata = metadataRepository
                        .findByObjectKey(objectKey)
                        .orElseThrow(() -> new StorageException("File not found"));

                if (!metadata.getUploadedBy().equals(userId)) {
                    throw new StorageException("Access denied");
                }

                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectKey)
                        .build());

                if (metadata.getThumbnailKey() != null) {
                    minioClient.removeObject(RemoveObjectArgs.builder()
                            .bucket(thumbnailBucket)
                            .object(metadata.getThumbnailKey())
                            .build());
                }

                metadataRepository.delete(metadata);

                log.info("Deleted file: {}", objectKey);
                metricsService.recordDeletion(objectKey, true);

            } catch (Exception e) {
                log.error("Delete failed for: {}", objectKey, e);
                metricsService.recordDeletion(objectKey, false);
                throw new StorageException("Delete failed", e);
            }
        });
    }

    @Async("storageTaskExecutor")
    @Transactional
    public CompletableFuture<Map<String, Boolean>> batchDeleteAsync(List<String> objectKeys, String userId) {

        return CompletableFuture.supplyAsync(() -> {
            Map<String, Boolean> results = new HashMap<>();

            try {
                List<FileMetadata> ownedFiles = metadataRepository.findByObjectKeyInAndUploadedBy(objectKeys, userId);

                List<DeleteObject> objectsToDelete = ownedFiles.stream()
                        .map(f -> new DeleteObject(f.getObjectKey()))
                        .collect(Collectors.toList());

                Iterable<Result<DeleteError>> deleteResults = minioClient.removeObjects(RemoveObjectsArgs.builder()
                        .bucket(bucket)
                        .objects(objectsToDelete)
                        .build());

                Set<String> failedDeletes = new HashSet<>();
                for (Result<DeleteError> result : deleteResults) {
                    DeleteError error = result.get();
                    failedDeletes.add(error.objectName());
                    log.error("Failed to delete: {} - {}", error.objectName(), error.message());
                }

                List<FileMetadata> successfulDeletes = ownedFiles.stream()
                        .filter(f -> !failedDeletes.contains(f.getObjectKey()))
                        .collect(Collectors.toList());

                metadataRepository.deleteAll(successfulDeletes);

                for (FileMetadata file : ownedFiles) {
                    results.put(file.getObjectKey(), !failedDeletes.contains(file.getObjectKey()));
                }

                return results;

            } catch (Exception e) {
                log.error("Batch delete failed", e);
                objectKeys.forEach(key -> results.put(key, false));
                return results;
            }
        });
    }

    public CompletableFuture<FileMetadata> getMetadataAsync(String objectKey, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            FileMetadata metadata = metadataRepository
                    .findByObjectKey(objectKey)
                    .orElseThrow(() -> new StorageException("File not found"));

            verifyFileAccess(objectKey, userId);
            return metadata;
        });
    }

    public CompletableFuture<List<FileMetadata>> listUserFilesAsync(String userId, int page, int size) {
        return CompletableFuture.supplyAsync(() -> metadataRepository.findByUploadedBy(
                userId, org.springframework.data.domain.PageRequest.of(page, size)));
    }

    @Transactional
    public CompletableFuture<Void> shareFileAsync(String objectKey, String ownerId, Set<String> sharedWithUsers) {

        return CompletableFuture.runAsync(() -> {
            FileMetadata metadata = metadataRepository
                    .findByObjectKey(objectKey)
                    .orElseThrow(() -> new StorageException("File not found"));

            if (!metadata.getUploadedBy().equals(ownerId)) {
                throw new StorageException("Only owner can share file");
            }

            metadata.getSharedWithUsers().addAll(sharedWithUsers);
            metadataRepository.save(metadata);

            log.info("File {} shared with {} users", objectKey, sharedWithUsers.size());
        });
    }

    private void uploadToMinio(MultipartFile file, String objectKey, String contentType) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucket).object(objectKey).stream(inputStream, file.getSize(), -1)
                            .contentType(contentType)
                            .build());
        }
    }

    private UploadResponse mergeChunks(
            String uploadId, int totalChunks, String filename, String contentType, String userId) throws Exception {

        String finalObjectKey = generateObjectKey(filename);
        File tempFile = File.createTempFile("merge-", ".tmp");

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            for (int i = 1; i <= totalChunks; i++) {
                String chunkKey = generateChunkKey(uploadId, i);
                try (InputStream is = minioClient.getObject(
                        GetObjectArgs.builder().bucket(bucket).object(chunkKey).build())) {
                    is.transferTo(fos);
                }
            }

            try (FileInputStream fis = new FileInputStream(tempFile)) {
                minioClient.putObject(
                        PutObjectArgs.builder().bucket(bucket).object(finalObjectKey).stream(fis, tempFile.length(), -1)
                                .contentType(contentType)
                                .build());
            }

            FileMetadata metadata = FileMetadata.builder()
                    .objectKey(finalObjectKey)
                    .originalFilename(filename)
                    .contentType(contentType)
                    .size(tempFile.length())
                    .uploadedBy(userId)
                    .uploadedAt(Instant.now())
                    .build();
            metadataRepository.save(metadata);

            deleteChunks(uploadId, totalChunks);
            triggerMediaProcessing(finalObjectKey, contentType);

            return UploadResponse.builder()
                    .objectKey(finalObjectKey)
                    .filename(filename)
                    .size(tempFile.length())
                    .contentType(contentType)
                    .complete(true)
                    .success(true)
                    .build();

        } finally {
            tempFile.delete();
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new FileValidationException(
                    String.format("File size exceeds maximum limit of %d bytes", maxFileSize));
        }

        String contentType = determineContentType(file);
        if (!isAllowedContentType(contentType)) {
            throw new FileValidationException("File type not allowed: " + contentType);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.contains("..") || filename.contains("/")) {
            throw new FileValidationException("Invalid filename");
        }
    }

    private void checkUserQuota(String userId, long fileSize) {
        long currentUsage = metadataRepository.getTotalSizeByUser(userId);

        if (currentUsage + fileSize > maxUserQuota) {
            throw new QuotaExceededException(
                    String.format("User quota exceeded. Current: %d, Limit: %d", currentUsage, maxUserQuota));
        }
    }

    private String determineContentType(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null || contentType.equals("application/octet-stream")) {
            String filename = file.getOriginalFilename();
            if (filename != null) {
                if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                    return "image/jpeg";
                } else if (filename.endsWith(".png")) {
                    return "image/png";
                } else if (filename.endsWith(".mp4")) {
                    return "video/mp4";
                }
            }
        }

        return contentType != null ? contentType : "application/octet-stream";
    }

    private boolean isAllowedContentType(String contentType) {
        return ALLOWED_IMAGE_TYPES.contains(contentType)
                || ALLOWED_VIDEO_TYPES.contains(contentType)
                || ALLOWED_DOCUMENT_TYPES.contains(contentType);
    }

    private String generateObjectKey(MultipartFile file) {
        String filename = file.getOriginalFilename();
        return generateObjectKey(filename);
    }

    private String generateObjectKey(String filename) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString();
        String sanitizedFilename = sanitizeFilename(filename);
        return String.format("%s/%s-%s", timestamp.substring(0, 10), uuid, sanitizedFilename);
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "file";
        }
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    private String generateChunkKey(String uploadId, int chunkNumber) {
        return String.format("chunks/%s/chunk-%04d", uploadId, chunkNumber);
    }

    private FileMetadata createFileMetadata(MultipartFile file, String objectKey, String contentType, String userId) {

        return FileMetadata.builder()
                .objectKey(objectKey)
                .originalFilename(file.getOriginalFilename())
                .contentType(contentType)
                .size(file.getSize())
                .uploadedBy(userId)
                .uploadedAt(Instant.now())
                .sharedWithUsers(new HashSet<>())
                .build();
    }

    private void verifyFileAccess(String objectKey, String userId) {
        FileMetadata metadata =
                metadataRepository.findByObjectKey(objectKey).orElseThrow(() -> new StorageException("File not found"));

        boolean hasAccess = metadata.getUploadedBy().equals(userId)
                || metadata.getSharedWithUsers().contains(userId);

        if (!hasAccess) {
            throw new StorageException("Access denied");
        }
    }

    private void triggerMediaProcessing(String objectKey, String contentType) {
        if (ALLOWED_IMAGE_TYPES.contains(contentType)) {
            mediaProcessingService.processImageAsync(objectKey);
        } else if (ALLOWED_VIDEO_TYPES.contains(contentType)) {
            mediaProcessingService.processVideoAsync(objectKey);
        }
    }

    private void cleanupFailedUpload(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception e) {
            log.error("Failed to cleanup after failed upload: {}", objectKey, e);
        }
    }

    private void deleteChunks(String uploadId, int totalChunks) {
        try {
            List<DeleteObject> chunks = new ArrayList<>();
            for (int i = 1; i <= totalChunks; i++) {
                chunks.add(new DeleteObject(generateChunkKey(uploadId, i)));
            }

            minioClient.removeObjects(
                    RemoveObjectsArgs.builder().bucket(bucket).objects(chunks).build());
        } catch (Exception e) {
            log.error("Failed to delete chunks for uploadId: {}", uploadId, e);
        }
    }
}
