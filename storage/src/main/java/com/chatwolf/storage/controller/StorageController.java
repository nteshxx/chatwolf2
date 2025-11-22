package com.chatwolf.storage.controller;

import com.chatwolf.storage.dto.ChunkUploadRequest;
import com.chatwolf.storage.dto.ShareFileRequest;
import com.chatwolf.storage.dto.UploadResponse;
import com.chatwolf.storage.entity.FileMetadata;
import com.chatwolf.storage.service.StorageService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public CompletableFuture<ResponseEntity<UploadResponse>> upload(
            @RequestParam MultipartFile file, @AuthenticationPrincipal UserDetails user) {

        return storageService
                .uploadAsync(file, user.getUsername())
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> ResponseEntity.internalServerError()
                        .body(UploadResponse.builder()
                                .success(false)
                                .message(ex.getMessage())
                                .build()));
    }

    @PostMapping("/upload/chunk")
    public CompletableFuture<ResponseEntity<UploadResponse>> uploadChunk(
            @Valid @ModelAttribute ChunkUploadRequest request,
            @RequestParam MultipartFile chunk,
            @AuthenticationPrincipal UserDetails user) {

        return storageService
                .uploadChunkAsync(request, chunk, user.getUsername())
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/presign/{objectKey}")
    public CompletableFuture<ResponseEntity<Map<String, String>>> getPresignedUrl(
            @PathVariable String objectKey, @AuthenticationPrincipal UserDetails user) {

        return storageService
                .getPresignedUrlAsync(objectKey, user.getUsername())
                .thenApply(url -> ResponseEntity.ok(Map.of("url", url)));
    }

    @GetMapping("/{objectKey}/download")
    public CompletableFuture<Void> download(
            @PathVariable String objectKey, @AuthenticationPrincipal UserDetails user, HttpServletResponse response) {

        return storageService.downloadAsync(objectKey, user.getUsername()).thenAccept(inputStream -> {
            try {
                inputStream.transferTo(response.getOutputStream());
                response.flushBuffer();
            } catch (Exception e) {
                throw new RuntimeException("Download failed", e);
            }
        });
    }

    @DeleteMapping("/{objectKey}")
    public CompletableFuture<ResponseEntity<Void>> delete(
            @PathVariable String objectKey, @AuthenticationPrincipal UserDetails user) {

        return storageService.deleteAsync(objectKey, user.getUsername()).thenApply(v -> ResponseEntity.noContent()
                .build());
    }

    @DeleteMapping("/batch")
    public CompletableFuture<ResponseEntity<Map<String, Boolean>>> batchDelete(
            @RequestBody List<String> objectKeys, @AuthenticationPrincipal UserDetails user) {

        return storageService.batchDeleteAsync(objectKeys, user.getUsername()).thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{objectKey}/metadata")
    public CompletableFuture<ResponseEntity<FileMetadata>> getMetadata(
            @PathVariable String objectKey, @AuthenticationPrincipal UserDetails user) {

        return storageService.getMetadataAsync(objectKey, user.getUsername()).thenApply(ResponseEntity::ok);
    }

    @GetMapping("/files")
    public CompletableFuture<ResponseEntity<List<FileMetadata>>> listFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails user) {

        return storageService.listUserFilesAsync(user.getUsername(), page, size).thenApply(ResponseEntity::ok);
    }

    @PostMapping("/{objectKey}/share")
    public CompletableFuture<ResponseEntity<Void>> shareFile(
            @PathVariable String objectKey,
            @Valid @RequestBody ShareFileRequest request,
            @AuthenticationPrincipal UserDetails user) {

        return storageService
                .shareFileAsync(objectKey, user.getUsername(), request.getUserIds())
                .thenApply(v -> ResponseEntity.ok().build());
    }
}
