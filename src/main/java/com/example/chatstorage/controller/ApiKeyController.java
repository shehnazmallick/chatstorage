package com.example.chatstorage.controller;

import com.example.chatstorage.dto.apikey.ApiKeyMetadataResponse;
import com.example.chatstorage.dto.apikey.CreateApiKeyRequest;
import com.example.chatstorage.dto.apikey.IssueApiKeyResponse;
import com.example.chatstorage.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/v1/api-keys")
@SecurityRequirement(name = "AdminApiKeyAuth")
@Validated
@Tag(name = "API Keys", description = "Admin endpoints to create, list, and revoke user API keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    @ResponseStatus(CREATED)
    @Operation(summary = "Create API key for a user", description = "Creates or rotates the API key for the target userId.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "API key created", content = @Content(schema = @Schema(implementation = IssueApiKeyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Invalid admin API key")
    })
    public IssueApiKeyResponse createApiKey(@Valid @RequestBody CreateApiKeyRequest request) {
        return apiKeyService.createApiKey(request);
    }

    @GetMapping
    @Operation(summary = "List API keys by user", description = "Returns API key metadata for the provided userId.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "API keys returned"),
            @ApiResponse(responseCode = "401", description = "Invalid admin API key")
    })
    public List<ApiKeyMetadataResponse> listKeys(@RequestParam @NotBlank String userId) {
        return apiKeyService.listByUser(userId);
    }

    @DeleteMapping("/{apiKeyId}")
    @ResponseStatus(NO_CONTENT)
    @Operation(summary = "Revoke API key", description = "Deactivates an API key by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "API key revoked"),
            @ApiResponse(responseCode = "401", description = "Invalid admin API key"),
            @ApiResponse(responseCode = "404", description = "API key not found")
    })
    public void revokeApiKey(@PathVariable UUID apiKeyId) {
        apiKeyService.revoke(apiKeyId);
    }
}
