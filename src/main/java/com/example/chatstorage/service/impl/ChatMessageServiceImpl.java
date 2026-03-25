package com.example.chatstorage.service.impl;

import com.example.chatstorage.dto.AddMessageRequest;
import com.example.chatstorage.dto.ChatMessageResponse;
import com.example.chatstorage.dto.PageResponse;
import com.example.chatstorage.entity.ChatMessage;
import com.example.chatstorage.entity.ChatSession;
import com.example.chatstorage.mapper.ChatMessageMapper;
import com.example.chatstorage.repository.ChatMessageRepository;
import com.example.chatstorage.service.ChatMessageService;
import com.example.chatstorage.service.ChatSessionService;
import com.example.chatstorage.service.support.AbstractPageableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ChatMessageServiceImpl extends AbstractPageableService implements ChatMessageService {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageServiceImpl.class);
    private static final int MAX_MESSAGE_PAGE_SIZE = 200;
    private static final Sort DEFAULT_MESSAGE_SORT = Sort.by(Sort.Direction.ASC, "createdAt");

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionService chatSessionService;
    private final ChatMessageMapper chatMessageMapper;

    public ChatMessageServiceImpl(ChatMessageRepository chatMessageRepository,
                                  ChatSessionService chatSessionService,
                                  ChatMessageMapper chatMessageMapper) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatSessionService = chatSessionService;
        this.chatMessageMapper = chatMessageMapper;
    }

    @Override
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
        log.info("Added chat message messageId={} sessionId={} userId={} sender={}",
                saved.getId(), sessionId, userId, saved.getSender());
        return chatMessageMapper.toResponse(saved);
    }

    @Override
    public PageResponse<ChatMessageResponse> listMessages(UUID sessionId, String userId, Pageable pageable) {
        chatSessionService.getSessionOrThrow(sessionId, userId);

        Pageable validatedPageable = validatePageable(pageable, MAX_MESSAGE_PAGE_SIZE, DEFAULT_MESSAGE_SORT);
        Page<ChatMessage> messages = chatMessageRepository.findBySessionId(sessionId, validatedPageable);
        log.debug("Listed chat messages sessionId={} userId={} page={} size={}",
                sessionId, userId, validatedPageable.getPageNumber(), validatedPageable.getPageSize());

        return PageResponse.fromPage(messages.map(chatMessageMapper::toResponse));
    }
}
