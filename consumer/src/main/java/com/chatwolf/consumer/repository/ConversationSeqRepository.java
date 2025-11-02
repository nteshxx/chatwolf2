package com.chatwolf.consumer.repository;

import com.chatwolf.consumer.entity.ConversationSeq;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationSeqRepository extends JpaRepository<ConversationSeq, String> {}
