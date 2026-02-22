package com.example.chatstorage.dto;

import com.example.chatstorage.entity.SenderType;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID sessionId,
        SenderType sender,
        String content,
        String retrievedContext,
        Instant createdAt
) {
}
