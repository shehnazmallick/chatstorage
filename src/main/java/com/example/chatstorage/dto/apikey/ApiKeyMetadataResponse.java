package com.example.chatstorage.dto.apikey;

import java.time.Instant;
import java.util.UUID;

public record ApiKeyMetadataResponse(
        UUID id,
        String userId,
        String name,
        String keyPrefix,
        boolean active,
        Instant createdAt,
        Instant lastUsedAt
) {
}
