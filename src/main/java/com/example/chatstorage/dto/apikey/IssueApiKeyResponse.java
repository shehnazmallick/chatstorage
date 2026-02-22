package com.example.chatstorage.dto.apikey;

import java.time.Instant;
import java.util.UUID;

public record IssueApiKeyResponse(
        UUID id,
        String userId,
        String name,
        String apiKey,
        Instant createdAt
) {
}
