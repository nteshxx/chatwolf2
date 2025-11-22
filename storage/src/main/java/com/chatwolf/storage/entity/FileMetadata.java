package com.chatwolf.storage.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.*;

@Entity
@Table(
        name = "t_file_metadata",
        indexes = {
            @Index(name = "idx_object_key", columnList = "objectKey"),
            @Index(name = "idx_uploaded_by", columnList = "uploadedBy"),
            @Index(name = "idx_uploaded_at", columnList = "uploadedAt")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 500)
    private String objectKey;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false, length = 100)
    private String uploadedBy;

    @Column(nullable = false)
    private Instant uploadedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "file_shares", joinColumns = @JoinColumn(name = "file_id"))
    @Column(name = "user_id")
    @Builder.Default
    private Set<String> sharedWithUsers = new HashSet<>();

    @Column(length = 500)
    private String thumbnailKey;

    @Column(length = 100)
    private String chatMessageId;

    @Column(length = 100)
    private String conversationId;

    // Image/Video metadata
    private Integer width;
    private Integer height;
    private Long duration; // in seconds for videos

    @Column(length = 50)
    private String status; // UPLOADED, PROCESSING, READY, FAILED

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
        if (status == null) {
            status = "UPLOADED";
        }
    }
}
