package com.example.chatstorage.auth;

import com.example.chatstorage.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthContextTest {

    @Test
    void requireUserIdShouldReturnValueWhenPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthContext.ATTR_USER_ID, "user-123");

        assertEquals("user-123", AuthContext.requireUserId(request));
    }

    @Test
    void requireUserIdShouldThrowWhenMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThrows(UnauthorizedException.class, () -> AuthContext.requireUserId(request));
    }
}
