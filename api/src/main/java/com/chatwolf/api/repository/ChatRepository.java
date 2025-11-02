package com.chatwolf.api.repository;

import com.chatwolf.api.entity.Chat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findByParticipantIdsContaining(String userId);
}
