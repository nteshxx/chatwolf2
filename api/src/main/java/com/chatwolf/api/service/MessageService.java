package com.chatwolf.api.service;

import com.chatwolf.api.dto.ConversationSummary;
import com.chatwolf.api.dto.MessagePageResponse;
import com.chatwolf.api.dto.MessageResponse;
import com.chatwolf.api.entity.Message;
import com.chatwolf.api.repository.MessageRepository;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private static final int PAGE_SIZE = 100;

    /**
     * Get conversation messages with infinite scroll
     */
    @Transactional(readOnly = true)
    public MessagePageResponse getConversationMessages(String conversationId, Long cursor) {

        List<Message> messages;

        if (cursor == null) {
            // First page - get latest messages
            messages = messageRepository.findFirstPageByConversation(conversationId, PageRequest.of(0, PAGE_SIZE));
        } else {
            // Next page - use cursor
            messages =
                    messageRepository.findNextPageByConversation(conversationId, cursor, PageRequest.of(0, PAGE_SIZE));
        }

        // Determine next cursor and hasMore flag
        Long nextCursor = null;
        boolean hasMore = messages.size() == PAGE_SIZE;

        if (hasMore && !messages.isEmpty()) {
            nextCursor = messages.get(messages.size() - 1).getSeqNo();
        }

        // Convert to DTOs
        List<MessageResponse> messageResponses =
                messages.stream().map(this::toMessageResponse).collect(Collectors.toList());

        return MessagePageResponse.builder()
                .messages(messageResponses)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .totalCount(messageRepository.countMessagesByConversation(conversationId))
                .build();
    }

    /**
     * Get user's conversation list
     */
    @Transactional(readOnly = true)
    public List<ConversationSummary> getUserConversations(String userId) {
        List<Object[]> results = messageRepository.findConversationListByUser(userId);

        return results.stream()
                .map(row -> ConversationSummary.builder()
                        .conversationId((String) row[0])
                        .lastMessageTime((Instant) row[1])
                        .lastMessagePreview((String) row[2])
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Search messages in conversation
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> searchMessages(String conversationId, String searchTerm) {

        List<Message> messages =
                messageRepository.searchMessagesInConversation(conversationId, searchTerm, PageRequest.of(0, 50));

        return messages.stream().map(this::toMessageResponse).collect(Collectors.toList());
    }

    /**
     * Get messages by date range (for exports, analytics)
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessagesByDateRange(String conversationId, Instant startDate, Instant endDate) {

        List<Message> messages = messageRepository.findMessagesByDateRange(conversationId, startDate, endDate);

        return messages.stream().map(this::toMessageResponse).collect(Collectors.toList());
    }

    private MessageResponse toMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .recipientId(message.getRecipientId())
                .content(message.getContent())
                .attachmentUrl(message.getAttachmentUrl())
                .seqNo(message.getSeqNo())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
