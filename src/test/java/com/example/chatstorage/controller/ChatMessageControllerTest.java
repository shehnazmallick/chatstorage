package com.example.chatstorage.controller;

import com.example.chatstorage.auth.AuthContext;
import com.example.chatstorage.dto.AddMessageRequest;
import com.example.chatstorage.dto.ChatMessageResponse;
import com.example.chatstorage.dto.PageResponse;
import com.example.chatstorage.entity.SenderType;
import com.example.chatstorage.service.ChatMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageControllerTest {

    @Mock
    private ChatMessageService chatMessageService;

    private ChatMessageController controller;

    @BeforeEach
    void setUp() {
        controller = new ChatMessageController(chatMessageService);
    }

    @Test
    void addMessageShouldPassUserContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthContext.ATTR_USER_ID, "user-1");
        UUID sessionId = UUID.randomUUID();
        AddMessageRequest body = new AddMessageRequest(SenderType.USER, "hi", null);

        ChatMessageResponse response = new ChatMessageResponse(
                UUID.randomUUID(), sessionId, SenderType.USER, "hi", null, Instant.now());
        when(chatMessageService.addMessage(sessionId, "user-1", body)).thenReturn(response);

        ChatMessageResponse actual = controller.addMessage(sessionId, body, request);

        assertEquals(sessionId, actual.sessionId());
        verify(chatMessageService).addMessage(sessionId, "user-1", body);
    }

    @Test
    void listMessagesShouldPassUserContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthContext.ATTR_USER_ID, "user-1");
        UUID sessionId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 50);
        // Empty typed page verifies controller wiring without relying on entity fixtures.
        PageResponse<ChatMessageResponse> page = new PageResponse<>(List.of(), 0, 50, 0, 0, false);

        when(chatMessageService.listMessages(sessionId, "user-1", pageable)).thenReturn(page);

        var actual = controller.listMessages(sessionId, request, pageable);

        assertEquals(0, actual.totalItems());
        verify(chatMessageService).listMessages(sessionId, "user-1", pageable);
    }
}
