package com.chatwolf.consumer.repository;

import com.chatwolf.consumer.entity.MessageEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    Optional<MessageEntity> findByEventId(String eventId);
}
