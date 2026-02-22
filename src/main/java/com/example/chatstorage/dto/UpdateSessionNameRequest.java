package com.example.chatstorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSessionNameRequest(
        @Schema(description = "New title for the session", example = "Spring Boot Best Practices")
        @NotBlank @Size(max = 200) String title
) {
}
