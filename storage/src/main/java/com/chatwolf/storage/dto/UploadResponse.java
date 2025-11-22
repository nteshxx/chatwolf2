package com.chatwolf.storage.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {
    private String objectKey;
    private String filename;
    private Long size;
    private String contentType;
    private Instant uploadedAt;
    private Boolean success;
    private String message;

    // For chunked uploads
    private String uploadId;
    private Integer chunkNumber;
    private Integer totalChunks;
    private Boolean complete;
}
