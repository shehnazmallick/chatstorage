package com.example.chatstorage.controller;

import com.example.chatstorage.dto.apikey.ApiKeyMetadataResponse;
import com.example.chatstorage.dto.apikey.CreateApiKeyRequest;
import com.example.chatstorage.dto.apikey.IssueApiKeyResponse;
import com.example.chatstorage.service.ApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyControllerTest {

    @Mock
    private ApiKeyService apiKeyService;

    private ApiKeyController controller;

    @BeforeEach
    void setUp() {
        controller = new ApiKeyController(apiKeyService);
    }

    @Test
    void createApiKeyShouldDelegate() {
        CreateApiKeyRequest request = new CreateApiKeyRequest("user-1", "name");
        IssueApiKeyResponse response = new IssueApiKeyResponse(
                UUID.randomUUID(), "user-1", "name", "csk_x.y", Instant.now());

        when(apiKeyService.createApiKey(request)).thenReturn(response);

        IssueApiKeyResponse actual = controller.createApiKey(request);

        assertEquals("user-1", actual.userId());
        verify(apiKeyService).createApiKey(request);
    }

    @Test
    void listKeysShouldDelegate() {
        var metadata = new ApiKeyMetadataResponse(
                UUID.randomUUID(), "user-1", "name", "pref", true, Instant.now(), null);
        when(apiKeyService.listByUser("user-1")).thenReturn(List.of(metadata));

        var actual = controller.listKeys("user-1");

        assertEquals(1, actual.size());
        verify(apiKeyService).listByUser("user-1");
    }

    @Test
    void revokeShouldDelegate() {
        UUID id = UUID.randomUUID();

        controller.revokeApiKey(id);

        verify(apiKeyService).revoke(id);
    }
}
