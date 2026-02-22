package com.example.chatstorage.service;

import com.example.chatstorage.dto.ChatSessionResponse;
import com.example.chatstorage.dto.CreateSessionRequest;
import com.example.chatstorage.dto.PageResponse;
import com.example.chatstorage.dto.UpdateFavoriteRequest;
import com.example.chatstorage.dto.UpdateSessionNameRequest;
import com.example.chatstorage.entity.ChatSession;
import com.example.chatstorage.exception.NotFoundException;
import com.example.chatstorage.repository.ChatMessageRepository;
import com.example.chatstorage.repository.ChatSessionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatSessionService(ChatSessionRepository chatSessionRepository, ChatMessageRepository chatMessageRepository) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    public ChatSessionResponse createSession(String userId, CreateSessionRequest request) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(request.title() == null || request.title().isBlank() ? "New Chat" : request.title().trim());
        ChatSession saved = chatSessionRepository.save(session);
        return toResponse(saved);
    }

    public PageResponse<ChatSessionResponse> listSessions(String userId, Boolean favorite, Pageable pageable) {
        Pageable validatedPageable = validatePageable(pageable, 100, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<ChatSession> sessions = (favorite == null)
                ? chatSessionRepository.findByUserId(userId, validatedPageable)
                : chatSessionRepository.findByUserIdAndFavorite(userId, favorite, validatedPageable);

        return PageResponse.fromPage(sessions.map(this::toResponse));
    }

    public ChatSessionResponse renameSession(UUID sessionId, String userId, UpdateSessionNameRequest request) {
        ChatSession session = getSessionOrThrow(sessionId, userId);
        session.setTitle(request.title().trim());
        return toResponse(chatSessionRepository.save(session));
    }

    public ChatSessionResponse updateFavorite(UUID sessionId, String userId, UpdateFavoriteRequest request) {
        ChatSession session = getSessionOrThrow(sessionId, userId);
        session.setFavorite(request.favorite());
        return toResponse(chatSessionRepository.save(session));
    }

    @Transactional
    public void deleteSession(UUID sessionId, String userId) {
        ChatSession session = getSessionOrThrow(sessionId, userId);
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.delete(session);
    }

    public ChatSession getSessionOrThrow(UUID sessionId, String userId) {
        return chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));
    }

    public void touchSession(ChatSession session) {
        chatSessionRepository.save(session);
    }

    private ChatSessionResponse toResponse(ChatSession session) {
        return new ChatSessionResponse(
                session.getId(),
                session.getUserId(),
                session.getTitle(),
                session.isFavorite(),
                session.getCreatedAt(),
                session.getUpdatedAt()
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
