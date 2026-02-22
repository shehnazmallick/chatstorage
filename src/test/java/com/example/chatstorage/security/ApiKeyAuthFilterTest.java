package com.example.chatstorage.security;

import com.example.chatstorage.auth.AuthContext;
import com.example.chatstorage.auth.AuthenticatedApiKey;
import com.example.chatstorage.config.AppProperties;
import com.example.chatstorage.exception.UnauthorizedException;
import com.example.chatstorage.service.ApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthFilterTest {

    @Mock
    private ApiKeyService apiKeyService;

    private ApiKeyAuthFilter filter;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getSecurity().setAdminApiKey("admin-secret");
        filter = new ApiKeyAuthFilter(properties, apiKeyService);
    }

    @Test
    void shouldBypassActuatorEndpoints() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        verifyNoInteractions(apiKeyService);
    }

    @Test
    void shouldRequireValidAdminKeyForApiKeyManagementEndpoints() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/api-keys");

        // API key lifecycle endpoints are admin-only and bypass user key auth.
        assertThrows(UnauthorizedException.class, () -> filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain()));
    }

    @Test
    void shouldAllowAdminKeyForApiKeyManagementEndpoints() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/api-keys");
        request.addHeader(ApiKeyAuthFilter.ADMIN_API_KEY_HEADER, "admin-secret");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verifyNoInteractions(apiKeyService);
    }

    @Test
    void shouldAuthenticateAndAttachContextForChatRead() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/sessions");
        request.addHeader(ApiKeyAuthFilter.API_KEY_HEADER, "csk_x.secret");

        AuthenticatedApiKey principal = new AuthenticatedApiKey(
                UUID.randomUUID(),
                "user-42",
                "x"
        );
        when(apiKeyService.authenticate("csk_x.secret")).thenReturn(principal);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(apiKeyService).authenticate("csk_x.secret");
        // Downstream controllers/services rely on request-scoped user identity.
        assertEquals("user-42", request.getAttribute(AuthContext.ATTR_USER_ID));
    }
}
