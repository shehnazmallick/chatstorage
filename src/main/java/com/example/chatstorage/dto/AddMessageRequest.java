package com.example.chatstorage.dto;

import com.example.chatstorage.entity.SenderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMessageRequest(
        @Schema(description = "Who sent this message (USER, ASSISTANT, or SYSTEM)", example = "USER")
        @NotNull SenderType sender,
        @Schema(description = "Message content", example = "What is Spring Boot?")
        @NotBlank String content,
        @Schema(description = "Optional context retrieved from RAG")
        String retrievedContext
) {
}
