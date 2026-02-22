package com.example.chatstorage.dto.apikey;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApiKeyRequest(
        @Schema(description = "User ID to create API key for", example = "user-alice-123")
        @NotBlank @Size(max = 100) String userId,
        @Schema(description = "Descriptive name for this API key", example = "web-client-prod")
        @NotBlank @Size(max = 120) String name
) {
}
