package com.example.chatstorage.service;

import com.example.chatstorage.dto.ChatSessionResponse;
import com.example.chatstorage.dto.CreateSessionRequest;
import com.example.chatstorage.dto.PageResponse;
import com.example.chatstorage.dto.UpdateFavoriteRequest;
import com.example.chatstorage.dto.UpdateSessionNameRequest;
import com.example.chatstorage.entity.ChatSession;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ChatSessionService {

    ChatSessionResponse createSession(String userId, CreateSessionRequest request);

    PageResponse<ChatSessionResponse> listSessions(String userId, Boolean favorite, Pageable pageable);

    ChatSessionResponse renameSession(UUID sessionId, String userId, UpdateSessionNameRequest request);

    ChatSessionResponse updateFavorite(UUID sessionId, String userId, UpdateFavoriteRequest request);

    void deleteSession(UUID sessionId, String userId);

    ChatSession getSessionOrThrow(UUID sessionId, String userId);

    void touchSession(ChatSession session);
}
