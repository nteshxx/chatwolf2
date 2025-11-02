package com.chatwolf.api.repository;

import com.chatwolf.api.entity.Message;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatIdOrderBySentAtAsc(Long chatId);

    List<Message> findTop100ByChatIdOrderBySentAtDesc(Long chatId);
}
