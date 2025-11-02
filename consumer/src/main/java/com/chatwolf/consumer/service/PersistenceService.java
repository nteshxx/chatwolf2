package com.chatwolf.consumer.service;

import com.chatwolf.consumer.dto.ChatMessageEvent;
import com.chatwolf.consumer.entity.ConversationSeq;
import com.chatwolf.consumer.entity.MessageEntity;
import com.chatwolf.consumer.repository.ConversationSeqRepository;
import com.chatwolf.consumer.repository.MessageRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersistenceService {

    private final MessageRepository messageRepository;
    private final ConversationSeqRepository seqRepository;

    @PersistenceContext
    private EntityManager em;

    public PersistenceService(MessageRepository messageRepository, ConversationSeqRepository seqRepository) {
        this.messageRepository = messageRepository;
        this.seqRepository = seqRepository;
    }

    @Transactional
    public MessageEntity persistMessage(ChatMessageEvent ev) {
        // Idempotency: if event already exists, return existing
        Optional<MessageEntity> existing = messageRepository.findByEventId(ev.getEventId());
        if (existing.isPresent()) {
            MessageEntity existingMessage = existing.get();
            existingMessage.setDuplicate(true);
            return existingMessage;
        }

        // Obtain or create conversation_seq row and lock it for update
        ConversationSeq seq = em.find(ConversationSeq.class, ev.getConversationId(), LockModeType.PESSIMISTIC_WRITE);
        if (seq == null) {
            seq = ConversationSeq.builder()
                    .conversationId(ev.getConversationId())
                    .lastSeq(0L)
                    .build();
            seqRepository.save(seq);
            // reload with lock
            seq = em.find(ConversationSeq.class, ev.getConversationId(), LockModeType.PESSIMISTIC_WRITE);
        }

        long nextSeq = seq.getLastSeq() + 1;
        seq.setLastSeq(nextSeq);
        seqRepository.save(seq);

        MessageEntity msg = MessageEntity.builder()
                .eventId(ev.getEventId())
                .clientMsgId(ev.getClientMsgId())
                .conversationId(ev.getConversationId())
                .senderId(ev.getSenderId())
                .recipientId(ev.getReceiverId())
                .content(ev.getContent())
                .attachmentUrl(ev.getAttachmentUrl())
                .seqNo(nextSeq)
                .createdAt(ev.getSentAt() == null ? Instant.now() : ev.getSentAt())
                .build();

        return messageRepository.save(msg);
    }
}
