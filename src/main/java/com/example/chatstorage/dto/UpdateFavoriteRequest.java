package com.example.chatstorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateFavoriteRequest(
        @Schema(description = "Whether to mark this session as favorite", example = "true")
        boolean favorite
) {
}
