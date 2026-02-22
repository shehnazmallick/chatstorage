package com.example.chatstorage.dto;

import com.example.chatstorage.entity.SenderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMessageRequest(
        @NotNull SenderType sender,
        @NotBlank String content,
        String retrievedContext
) {
}
