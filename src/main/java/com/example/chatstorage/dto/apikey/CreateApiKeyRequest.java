package com.example.chatstorage.dto.apikey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApiKeyRequest(
        @NotBlank @Size(max = 100) String userId,
        @NotBlank @Size(max = 120) String name
) {
}
