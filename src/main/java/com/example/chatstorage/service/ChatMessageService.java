package com.example.chatstorage.service;

import com.example.chatstorage.dto.AddMessageRequest;
import com.example.chatstorage.dto.ChatMessageResponse;
import com.example.chatstorage.dto.PageResponse;
import com.example.chatstorage.entity.ChatMessage;
import com.example.chatstorage.entity.ChatSession;
import com.example.chatstorage.repository.ChatMessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionService chatSessionService;

    public ChatMessageService(ChatMessageRepository chatMessageRepository, ChatSessionService chatSessionService) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatSessionService = chatSessionService;
    }

    @Transactional
    public ChatMessageResponse addMessage(UUID sessionId, String userId, AddMessageRequest request) {
        ChatSession session = chatSessionService.getSessionOrThrow(sessionId, userId);
        session.setUpdatedAt(Instant.now());
        chatSessionService.touchSession(session);

        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setSender(request.sender());
        message.setContent(request.content().trim());
        message.setRetrievedContext(request.retrievedContext());

        ChatMessage saved = chatMessageRepository.save(message);
        return toResponse(saved);
    }

    public PageResponse<ChatMessageResponse> listMessages(UUID sessionId, String userId, Pageable pageable) {
        chatSessionService.getSessionOrThrow(sessionId, userId);

        Pageable validatedPageable = validatePageable(pageable, 200, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<ChatMessage> messages = chatMessageRepository.findBySessionId(sessionId, validatedPageable);

        return PageResponse.fromPage(messages.map(this::toResponse));
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getSession().getId(),
                message.getSender(),
                message.getContent(),
                message.getRetrievedContext(),
                message.getCreatedAt()
        );
    }

    private Pageable validatePageable(Pageable pageable, int maxSize, Sort defaultSort) {
        int size = pageable.getPageSize();
        if (size < 1 || size > maxSize) {
            throw new IllegalArgumentException("size must be between 1 and " + maxSize);
        }

        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : defaultSort;
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }
}
