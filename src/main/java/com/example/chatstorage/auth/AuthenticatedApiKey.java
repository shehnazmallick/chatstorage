package com.example.chatstorage.auth;

import java.util.UUID;

public record AuthenticatedApiKey(
        UUID id,
        String userId,
        String keyPrefix
) {
}
