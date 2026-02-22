package com.example.chatstorage.controller;

import com.example.chatstorage.auth.AuthContext;
import com.example.chatstorage.dto.ChatSessionResponse;
import com.example.chatstorage.dto.CreateSessionRequest;
import com.example.chatstorage.dto.PageResponse;
import com.example.chatstorage.dto.UpdateFavoriteRequest;
import com.example.chatstorage.dto.UpdateSessionNameRequest;
import com.example.chatstorage.service.ChatSessionService;
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
class ChatSessionControllerTest {

    @Mock
    private ChatSessionService chatSessionService;

    private ChatSessionController controller;

    @BeforeEach
    void setUp() {
        controller = new ChatSessionController(chatSessionService);
    }

    @Test
    void createSessionShouldUseAuthContextUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthContext.ATTR_USER_ID, "user-1");

        ChatSessionResponse response = new ChatSessionResponse(
                UUID.randomUUID(), "user-1", "t", false, Instant.now(), Instant.now());
        when(chatSessionService.createSession("user-1", new CreateSessionRequest("Title"))).thenReturn(response);

        ChatSessionResponse actual = controller.createSession(new CreateSessionRequest("Title"), request);

        assertEquals("user-1", actual.userId());
        verify(chatSessionService).createSession("user-1", new CreateSessionRequest("Title"));
    }

    @Test
    void listSessionsShouldUseAuthContextUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthContext.ATTR_USER_ID, "user-1");
        var pageable = PageRequest.of(0, 20);
        PageResponse<ChatSessionResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0, false);
        when(chatSessionService.listSessions("user-1", true, pageable)).thenReturn(page);

        var actual = controller.listSessions(true, request, pageable);

        assertEquals(0, actual.totalItems());
        verify(chatSessionService).listSessions("user-1", true, pageable);
    }

    @Test
    void renameSessionShouldPassUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthContext.ATTR_USER_ID, "user-1");
        UUID sessionId = UUID.randomUUID();

        controller.renameSession(sessionId, new UpdateSessionNameRequest("new"), request);

        verify(chatSessionService).renameSession(sessionId, "user-1", new UpdateSessionNameRequest("new"));
    }

    @Test
    void updateFavoriteShouldPassUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthContext.ATTR_USER_ID, "user-1");
        UUID sessionId = UUID.randomUUID();

        controller.updateFavorite(sessionId, new UpdateFavoriteRequest(true), request);

        verify(chatSessionService).updateFavorite(sessionId, "user-1", new UpdateFavoriteRequest(true));
    }

    @Test
    void deleteSessionShouldPassUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthContext.ATTR_USER_ID, "user-1");
        UUID sessionId = UUID.randomUUID();

        controller.deleteSession(sessionId, request);

        verify(chatSessionService).deleteSession(sessionId, "user-1");
    }
}
