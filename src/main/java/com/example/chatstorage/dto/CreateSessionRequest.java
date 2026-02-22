package com.example.chatstorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
        @Schema(description = "Title or topic for this chat session", example = "Discussion about Spring Boot")
        @Size(max = 200) String title
) {
}
