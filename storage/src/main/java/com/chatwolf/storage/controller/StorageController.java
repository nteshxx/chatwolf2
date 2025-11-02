package com.chatwolf.storage.controller;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
public class StorageController {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    String bucket;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(@RequestParam MultipartFile file) throws Exception {
        String objectName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucket).object(objectName).stream(is, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
        }
        // Optionally persist metadata to DB
        return ResponseEntity.ok(Map.of("objectKey", objectName));
    }

    @GetMapping("/presign/{objectKey}")
    public ResponseEntity<Map<String, String>> presign(@PathVariable String objectKey) throws Exception {
        var presigned = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(objectKey)
                .expiry(60 * 60) // 1 hour
                .build());
        return ResponseEntity.ok(Map.of("url", presigned));
    }

    @GetMapping("/{objectKey}")
    public void download(@PathVariable String objectKey, HttpServletResponse resp) throws Exception {
        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(objectKey).build())) {
            IOUtils.copy(is, resp.getOutputStream());
            resp.flushBuffer();
        }
    }
}
