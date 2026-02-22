package com.example.chatstorage.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatSessionResponse(
        UUID id,
        String userId,
        String title,
        boolean favorite,
        Instant createdAt,
        Instant updatedAt
) {
}
