package com.example.chatstorage.controller;

import com.example.chatstorage.auth.AuthContext;
import com.example.chatstorage.dto.ChatSessionResponse;
import com.example.chatstorage.dto.CreateSessionRequest;
import com.example.chatstorage.dto.PageResponse;
import com.example.chatstorage.dto.UpdateFavoriteRequest;
import com.example.chatstorage.dto.UpdateSessionNameRequest;
import com.example.chatstorage.service.ChatSessionService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/v1/sessions")
@Validated
@SecurityRequirement(name = "ApiKeyAuth")
@Tag(name = "Sessions", description = "Manage chat sessions for the authenticated user")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    public ChatSessionController(ChatSessionService chatSessionService) {
        this.chatSessionService = chatSessionService;
    }

    @PostMapping
    @ResponseStatus(CREATED)
    @Operation(summary = "Create session")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Session created", content = @Content(schema = @Schema(implementation = ChatSessionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid user API key")
    })
    public ChatSessionResponse createSession(@Valid @RequestBody CreateSessionRequest request,
                                             HttpServletRequest httpRequest) {
        String userId = AuthContext.requireUserId(httpRequest);
        return chatSessionService.createSession(userId, request);
    }

    @GetMapping
    @Operation(summary = "List sessions", description = "Returns paginated sessions for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessions returned"),
            @ApiResponse(responseCode = "401", description = "Invalid user API key")
    })
    public PageResponse<ChatSessionResponse> listSessions(
            @Parameter(description = "Filter favorites only when true; all sessions when omitted")
            @RequestParam(required = false) Boolean favorite,
            HttpServletRequest httpRequest,
            @Parameter(description = "Pagination and sorting (size max 100)")
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        String userId = AuthContext.requireUserId(httpRequest);
        return chatSessionService.listSessions(userId, favorite, pageable);
    }

    @PatchMapping("/{sessionId}/rename")
    @Operation(summary = "Rename session")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session renamed"),
            @ApiResponse(responseCode = "401", description = "Invalid user API key"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ChatSessionResponse renameSession(@PathVariable UUID sessionId,
                                             @Valid @RequestBody UpdateSessionNameRequest request,
                                             HttpServletRequest httpRequest) {
        String userId = AuthContext.requireUserId(httpRequest);
        return chatSessionService.renameSession(sessionId, userId, request);
    }

    @PatchMapping("/{sessionId}/favorite")
    @Operation(summary = "Mark or unmark session as favorite")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session updated"),
            @ApiResponse(responseCode = "401", description = "Invalid user API key"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ChatSessionResponse updateFavorite(@PathVariable UUID sessionId,
                                              @Valid @RequestBody UpdateFavoriteRequest request,
                                              HttpServletRequest httpRequest) {
        String userId = AuthContext.requireUserId(httpRequest);
        return chatSessionService.updateFavorite(sessionId, userId, request);
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(NO_CONTENT)
    @Operation(summary = "Delete session")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Session deleted"),
            @ApiResponse(responseCode = "401", description = "Invalid user API key"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public void deleteSession(@PathVariable UUID sessionId, HttpServletRequest httpRequest) {
        String userId = AuthContext.requireUserId(httpRequest);
        chatSessionService.deleteSession(sessionId, userId);
    }
}
