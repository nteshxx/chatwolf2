package com.chatwolf.consumer.service;

import com.chatwolf.consumer.dto.ChatMessageEvent;
import com.chatwolf.consumer.entity.Message;
import com.chatwolf.consumer.repository.MessageRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {

    @PersistenceContext
    private EntityManager entityManager;

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Transactional
    public Message saveMessage(ChatMessageEvent messageEvent) {
        // check for duplicate socket event id
        Optional<Message> existingEvent = messageRepository.findByEventId(messageEvent.getEventId());
        if (existingEvent.isPresent()) {
            Message existingMessage = existingEvent.get();
            existingMessage.setDuplicate(true);
            return existingMessage;
        }

        // check for duplicate client message id
        Optional<Message> existingClientMsg = messageRepository.findByClientMsgId(messageEvent.getEventId());
        if (existingClientMsg.isPresent()) {
            Message existingMessage = existingClientMsg.get();
            existingMessage.setDuplicate(true);
            return existingMessage;
        }

        // get next sequence number using database function
        Long nextSeq = messageRepository.getNextSeqNo(messageEvent.getConversationId());

        Message msg = Message.builder()
                .eventId(messageEvent.getEventId())
                .clientMsgId(messageEvent.getClientMsgId())
                .conversationId(messageEvent.getConversationId())
                .senderId(messageEvent.getFrom())
                .recipientId(messageEvent.getTo())
                .content(messageEvent.getContent())
                .attachmentUrl(messageEvent.getAttachmentUrl())
                .seqNo(nextSeq)
                .createdAt(messageEvent.getSentAt() == null ? Instant.now() : messageEvent.getSentAt())
                .build();

        return messageRepository.save(msg);
    }

    @Transactional
    public List<Message> saveMessageBatch(List<ChatMessageEvent> messageEventList) {
        List<Message> savedMessages = new ArrayList<>();
        for (ChatMessageEvent messageEvent : messageEventList) {
            savedMessages.add(saveMessage(messageEvent));
        }
        return savedMessages;
    }
}
