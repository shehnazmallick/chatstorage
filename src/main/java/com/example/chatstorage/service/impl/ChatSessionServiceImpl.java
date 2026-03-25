package com.example.chatstorage.service.impl;

import com.example.chatstorage.dto.ChatSessionResponse;
import com.example.chatstorage.dto.CreateSessionRequest;
import com.example.chatstorage.dto.PageResponse;
import com.example.chatstorage.dto.UpdateFavoriteRequest;
import com.example.chatstorage.dto.UpdateSessionNameRequest;
import com.example.chatstorage.entity.ChatSession;
import com.example.chatstorage.exception.NotFoundException;
import com.example.chatstorage.mapper.ChatSessionMapper;
import com.example.chatstorage.repository.ChatMessageRepository;
import com.example.chatstorage.repository.ChatSessionRepository;
import com.example.chatstorage.service.ChatSessionService;
import com.example.chatstorage.service.support.AbstractPageableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ChatSessionServiceImpl extends AbstractPageableService implements ChatSessionService {

    private static final Logger log = LoggerFactory.getLogger(ChatSessionServiceImpl.class);
    private static final String DEFAULT_SESSION_TITLE = "New Chat";
    private static final int MAX_SESSION_PAGE_SIZE = 100;
    private static final Sort DEFAULT_SESSION_SORT = Sort.by(Sort.Direction.DESC, "updatedAt");

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionMapper chatSessionMapper;

    public ChatSessionServiceImpl(ChatSessionRepository chatSessionRepository,
                                  ChatMessageRepository chatMessageRepository,
                                  ChatSessionMapper chatSessionMapper) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatSessionMapper = chatSessionMapper;
    }

    @Override
    @Transactional
    public ChatSessionResponse createSession(String userId, CreateSessionRequest request) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(resolveSessionTitle(request));
        ChatSession saved = chatSessionRepository.save(session);
        log.info("Created chat session sessionId={} userId={}", saved.getId(), saved.getUserId());
        return chatSessionMapper.toResponse(saved);
    }

    @Override
    public PageResponse<ChatSessionResponse> listSessions(String userId, Boolean favorite, Pageable pageable) {
        Pageable validatedPageable = validatePageable(pageable, MAX_SESSION_PAGE_SIZE, DEFAULT_SESSION_SORT);
        Page<ChatSession> sessions = (favorite == null)
                ? chatSessionRepository.findByUserId(userId, validatedPageable)
                : chatSessionRepository.findByUserIdAndFavorite(userId, favorite, validatedPageable);

        log.debug("Listed chat sessions userId={} favorite={} page={} size={}",
                userId, favorite, validatedPageable.getPageNumber(), validatedPageable.getPageSize());
        return PageResponse.fromPage(sessions.map(chatSessionMapper::toResponse));
    }

    @Override
    @Transactional
    public ChatSessionResponse renameSession(UUID sessionId, String userId, UpdateSessionNameRequest request) {
        ChatSession session = getSessionOrThrow(sessionId, userId);
        session.setTitle(request.title().trim());
        ChatSession saved = chatSessionRepository.save(session);
        log.info("Renamed chat session sessionId={} userId={}", saved.getId(), saved.getUserId());
        return chatSessionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ChatSessionResponse updateFavorite(UUID sessionId, String userId, UpdateFavoriteRequest request) {
        ChatSession session = getSessionOrThrow(sessionId, userId);
        session.setFavorite(request.favorite());
        ChatSession saved = chatSessionRepository.save(session);
        log.info("Updated favorite flag sessionId={} userId={} favorite={}",
                saved.getId(), saved.getUserId(), saved.isFavorite());
        return chatSessionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteSession(UUID sessionId, String userId) {
        ChatSession session = getSessionOrThrow(sessionId, userId);
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.delete(session);
        log.info("Deleted chat session sessionId={} userId={}", sessionId, userId);
    }

    @Override
    public ChatSession getSessionOrThrow(UUID sessionId, String userId) {
        return chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));
    }

    @Override
    @Transactional
    public void touchSession(ChatSession session) {
        chatSessionRepository.save(session);
        log.debug("Touched chat session sessionId={} userId={}", session.getId(), session.getUserId());
    }

    private String resolveSessionTitle(CreateSessionRequest request) {
        String title = request.title();
        return (title == null || title.isBlank()) ? DEFAULT_SESSION_TITLE : title.trim();
    }
}
