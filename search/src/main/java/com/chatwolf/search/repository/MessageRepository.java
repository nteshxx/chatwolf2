package com.chatwolf.search.repository;

import com.chatwolf.search.entity.MessageDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface MessageRepository extends ElasticsearchRepository<MessageDocument, String> {
    Page<MessageDocument> findByConversationId(String conversationId, Pageable pageable);
}
