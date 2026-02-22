package com.example.chatstorage.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private RedisRateLimiterService redisRateLimiterService;

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter(redisRateLimiterService);
    }

    @Test
    void shouldBypassActuatorEndpoints() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verifyNoInteractions(redisRateLimiterService);
    }

    @Test
    void shouldUseApiKeyAndForwardedIpForFingerprint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/sessions");
        request.addHeader(ApiKeyAuthFilter.API_KEY_HEADER, "client-key");
        request.addHeader("X-Forwarded-For", "198.51.100.10, 10.0.0.1");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redisRateLimiterService).acquirePermission(captor.capture());
        assertEquals(sha256("client-key:198.51.100.10"), captor.getValue());
    }

    @Test
    void shouldFallbackToAdminKeyForFingerprint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/api-keys");
        request.setRemoteAddr("203.0.113.7");
        request.addHeader(ApiKeyAuthFilter.ADMIN_API_KEY_HEADER, "admin-key");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redisRateLimiterService).acquirePermission(captor.capture());
        assertEquals(sha256("admin-key:203.0.113.7"), captor.getValue());
    }

    private String sha256(String input) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
