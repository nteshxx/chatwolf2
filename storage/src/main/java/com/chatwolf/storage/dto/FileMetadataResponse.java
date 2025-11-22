package com.chatwolf.storage.dto;

import java.time.Instant;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadataResponse {
    private String objectKey;
    private String originalFilename;
    private String contentType;
    private Long size;
    private String uploadedBy;
    private Instant uploadedAt;
    private String thumbnailUrl;
    private Set<String> sharedWithUsers;
    private String downloadUrl;
}
