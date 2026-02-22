package com.example.chatstorage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSessionNameRequest(
        @NotBlank @Size(max = 200) String title
) {
}
