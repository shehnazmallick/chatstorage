package com.example.chatstorage.security;

import com.example.chatstorage.auth.AuthContext;
import com.example.chatstorage.auth.AuthenticatedApiKey;
import com.example.chatstorage.config.AppProperties;
import com.example.chatstorage.exception.UnauthorizedException;
import com.example.chatstorage.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
@Order(2)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String ADMIN_API_KEY_HEADER = "X-Admin-Key";
    private static final String API_KEY_PATH = "/api/v1/api-keys";
    private final AppProperties appProperties;
    private final ApiKeyService apiKeyService;

    public ApiKeyAuthFilter(AppProperties appProperties, ApiKeyService apiKeyService) {
        this.appProperties = appProperties;
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri.startsWith("/actuator") || uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (uri.startsWith(API_KEY_PATH)) {
            validateAdminApiKey(request);
            filterChain.doFilter(request, response);
            return;
        }

        String providedApiKey = request.getHeader(API_KEY_HEADER);
        AuthenticatedApiKey authenticatedApiKey = apiKeyService.authenticate(providedApiKey);
        request.setAttribute(AuthContext.ATTR_USER_ID, authenticatedApiKey.userId());

        filterChain.doFilter(request, response);
    }

    private void validateAdminApiKey(HttpServletRequest request) {
        String configuredAdminApiKey = appProperties.getSecurity().getAdminApiKey();
        if (configuredAdminApiKey == null || configuredAdminApiKey.isBlank()) {
            throw new UnauthorizedException("Admin API key is not configured on server");
        }

        String provided = request.getHeader(ADMIN_API_KEY_HEADER);
        if (!configuredAdminApiKey.equals(provided)) {
            throw new UnauthorizedException("Invalid admin API key");
        }
    }
}
