package com.chatwolf.storage.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChunkUploadRequest {
    @NotBlank
    private String uploadId;

    @Min(1)
    private Integer chunkNumber;

    @Min(1)
    private Integer totalChunks;

    @NotBlank
    private String filename;

    private String contentType;
}
