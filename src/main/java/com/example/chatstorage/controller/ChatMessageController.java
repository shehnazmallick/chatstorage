package com.example.chatstorage.controller;

import com.example.chatstorage.auth.AuthContext;
import com.example.chatstorage.dto.AddMessageRequest;
import com.example.chatstorage.dto.ChatMessageResponse;
import com.example.chatstorage.dto.PageResponse;
import com.example.chatstorage.service.ChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/messages")
@Validated
@SecurityRequirement(name = "ApiKeyAuth")
@Tag(name = "Messages", description = "Store and retrieve chat messages for a session")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    public ChatMessageController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @PostMapping
    @ResponseStatus(CREATED)
    @Operation(summary = "Add message to a session")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Message added", content = @Content(schema = @Schema(implementation = ChatMessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid user API key"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ChatMessageResponse addMessage(@PathVariable UUID sessionId,
                                          @Valid @RequestBody AddMessageRequest request,
                                          HttpServletRequest httpRequest) {
        String userId = AuthContext.requireUserId(httpRequest);
        return chatMessageService.addMessage(sessionId, userId, request);
    }

    @GetMapping
    @Operation(summary = "Get message history", description = "Returns paginated message history for the session.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messages returned"),
            @ApiResponse(responseCode = "401", description = "Invalid user API key"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public PageResponse<ChatMessageResponse> listMessages(
            @PathVariable UUID sessionId,
            HttpServletRequest httpRequest,
            @Parameter(description = "Pagination and sorting (size max 200)")
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        String userId = AuthContext.requireUserId(httpRequest);
        return chatMessageService.listMessages(sessionId, userId, pageable);
    }
}
