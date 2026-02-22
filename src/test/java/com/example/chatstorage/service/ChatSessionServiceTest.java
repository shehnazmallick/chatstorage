package com.example.chatstorage.service;

import com.example.chatstorage.dto.CreateSessionRequest;
import com.example.chatstorage.dto.UpdateFavoriteRequest;
import com.example.chatstorage.dto.UpdateSessionNameRequest;
import com.example.chatstorage.entity.ChatSession;
import com.example.chatstorage.exception.NotFoundException;
import com.example.chatstorage.repository.ChatMessageRepository;
import com.example.chatstorage.repository.ChatSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSessionServiceTest {

    @Mock
    private ChatSessionRepository sessionRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    private ChatSessionService service;

    @BeforeEach
    void setUp() {
        service = new ChatSessionService(sessionRepository, messageRepository);
    }

    @Test
    void createSessionShouldUseAuthenticatedUserAndDefaultTitle() {
        when(sessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> {
            ChatSession session = invocation.getArgument(0);
            session.setId(UUID.randomUUID());
            return session;
        });

        var response = service.createSession("user-1", new CreateSessionRequest(""));

        assertEquals("New Chat", response.title());
        assertEquals("user-1", response.userId());
    }

    @Test
    void renameSessionShouldUpdateTitle() {
        UUID sessionId = UUID.randomUUID();
        ChatSession session = new ChatSession();
        session.setId(sessionId);
        session.setTitle("Old");

        when(sessionRepository.findByIdAndUserId(sessionId, "user-1")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.renameSession(sessionId, "user-1", new UpdateSessionNameRequest(" New Title "));

        assertEquals("New Title", response.title());
    }

    @Test
    void updateFavoriteShouldToggleFlag() {
        UUID sessionId = UUID.randomUUID();
        ChatSession session = new ChatSession();
        session.setId(sessionId);
        session.setFavorite(false);

        when(sessionRepository.findByIdAndUserId(sessionId, "user-1")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateFavorite(sessionId, "user-1", new UpdateFavoriteRequest(true));

        assertTrue(response.favorite());
    }

    @Test
    void deleteSessionShouldDeleteMessagesThenSession() {
        UUID sessionId = UUID.randomUUID();
        ChatSession session = new ChatSession();
        session.setId(sessionId);

        when(sessionRepository.findByIdAndUserId(sessionId, "user-1")).thenReturn(Optional.of(session));

        service.deleteSession(sessionId, "user-1");

        verify(messageRepository).deleteBySessionId(sessionId);
        verify(sessionRepository).delete(session);
    }

    @Test
    void getSessionOrThrowShouldThrowWhenMissing() {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findByIdAndUserId(sessionId, "user-1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getSessionOrThrow(sessionId, "user-1"));
    }

    @Test
    void listSessionsShouldUseFavoriteFilterWhenProvided() {
        ChatSession session = new ChatSession();
        session.setId(UUID.randomUUID());
        session.setUserId("user-1");
        session.setTitle("A");

        // API default contract expects newest sessions first.
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt"));
        when(sessionRepository.findByUserIdAndFavorite("user-1", true, pageable))
                .thenReturn(new PageImpl<>(java.util.List.of(session), pageable, 1));

        var response = service.listSessions("user-1", true, pageable);

        assertEquals(1, response.totalItems());
    }

    @Test
    void listSessionsShouldThrowOnInvalidPageSize() {
        var pageable = PageRequest.of(0, 101);

        // Guardrail to prevent unbounded/high-cost page sizes.
        assertThrows(IllegalArgumentException.class, () -> service.listSessions("user-1", null, pageable));
    }

    @Test
    void touchSessionShouldPersistSession() {
        ChatSession session = new ChatSession();
        service.touchSession(session);
        verify(sessionRepository).save(session);
    }
}
