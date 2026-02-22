package com.example.chatstorage.auth;

import com.example.chatstorage.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;

public final class AuthContext {

    public static final String ATTR_USER_ID = "auth.userId";

    private AuthContext() {
    }

    public static String requireUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(ATTR_USER_ID);
        if (userId instanceof String value && !value.isBlank()) {
            return value;
        }
        throw new UnauthorizedException("Authenticated user context missing");
    }
}
