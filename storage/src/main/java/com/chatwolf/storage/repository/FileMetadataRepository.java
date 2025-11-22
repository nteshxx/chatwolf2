package com.chatwolf.storage.repository;

import com.chatwolf.storage.entity.FileMetadata;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    Optional<FileMetadata> findByObjectKey(String objectKey);

    List<FileMetadata> findByUploadedBy(String userId, Pageable pageable);

    List<FileMetadata> findByObjectKeyInAndUploadedBy(List<String> objectKeys, String userId);

    @Query("SELECT COALESCE(SUM(f.size), 0) FROM FileMetadata f WHERE f.uploadedBy = :userId")
    long getTotalSizeByUser(@Param("userId") String userId);

    @Query("SELECT f FROM FileMetadata f WHERE f.uploadedBy = :userId " + "OR :userId MEMBER OF f.sharedWithUsers")
    List<FileMetadata> findAccessibleByUser(@Param("userId") String userId, Pageable pageable);

    List<FileMetadata> findByConversationId(String conversationId);

    List<FileMetadata> findByChatMessageId(String chatMessageId);

    @Query("SELECT f FROM FileMetadata f WHERE f.uploadedAt < :before AND f.status = 'UPLOADED'")
    List<FileMetadata> findOldFiles(@Param("before") Instant before);

    long countByUploadedBy(String userId);

    void deleteByObjectKey(String objectKey);
}
