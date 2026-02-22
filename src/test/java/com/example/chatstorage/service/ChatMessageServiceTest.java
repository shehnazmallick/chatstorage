package com.example.chatstorage.service;

import com.example.chatstorage.dto.AddMessageRequest;
import com.example.chatstorage.entity.ChatMessage;
import com.example.chatstorage.entity.ChatSession;
import com.example.chatstorage.entity.SenderType;
import com.example.chatstorage.repository.ChatMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private ChatSessionService sessionService;

    private ChatMessageService messageService;

    @BeforeEach
    void setUp() {
        messageService = new ChatMessageService(messageRepository, sessionService);
    }

    @Test
    void addMessageShouldPersistMessageWithTrimmedContent() {
        UUID sessionId = UUID.randomUUID();
        ChatSession session = new ChatSession();
        session.setId(sessionId);

        when(sessionService.getSessionOrThrow(sessionId, "user-1")).thenReturn(session);
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setId(UUID.randomUUID());
            return message;
        });

        var response = messageService.addMessage(sessionId, "user-1",
                new AddMessageRequest(SenderType.USER, " hello ", "ctx"));

        assertEquals(sessionId, response.sessionId());
        assertEquals(SenderType.USER, response.sender());
        assertEquals("hello", response.content());
        assertEquals("ctx", response.retrievedContext());
        verify(messageRepository).save(any(ChatMessage.class));
    }

    @Test
    void listMessagesShouldReturnPageData() {
        UUID sessionId = UUID.randomUUID();
        ChatSession session = new ChatSession();
        session.setId(sessionId);
        when(sessionService.getSessionOrThrow(sessionId, "user-1")).thenReturn(session);

        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID());
        message.setSession(session);
        message.setSender(SenderType.ASSISTANT);
        message.setContent("answer");

        var pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "createdAt"));
        when(messageRepository.findBySessionId(sessionId, pageable))
                .thenReturn(new PageImpl<>(java.util.List.of(message), pageable, 1));

        var response = messageService.listMessages(sessionId, "user-1", pageable);

        assertEquals(1, response.totalItems());
        assertEquals("answer", response.items().get(0).content());
    }

    @Test
    void listMessagesShouldThrowOnInvalidPageSize() {
        UUID sessionId = UUID.randomUUID();
        ChatSession session = new ChatSession();
        session.setId(sessionId);
        when(sessionService.getSessionOrThrow(sessionId, "user-1")).thenReturn(session);

        assertThrows(IllegalArgumentException.class,
                () -> messageService.listMessages(sessionId, "user-1", PageRequest.of(0, 201)));
    }
}
