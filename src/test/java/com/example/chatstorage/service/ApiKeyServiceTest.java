package com.example.chatstorage.service;

import com.example.chatstorage.auth.AuthenticatedApiKey;
import com.example.chatstorage.config.AppProperties;
import com.example.chatstorage.dto.apikey.CreateApiKeyRequest;
import com.example.chatstorage.entity.ApiKey;
import com.example.chatstorage.exception.NotFoundException;
import com.example.chatstorage.exception.UnauthorizedException;
import com.example.chatstorage.repository.ApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    private ApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getSecurity().setApiKeyPepper("pepper-secret");
        apiKeyService = new ApiKeyService(apiKeyRepository, properties);
    }

    @Test
    void createApiKeyShouldPersistHashAndReturnPlaintextKeyOnce() {
        when(apiKeyRepository.findByUserId("user-1")).thenReturn(Optional.empty());
        // Simulate DB-generated fields so response mapping can be asserted.
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> {
            ApiKey apiKey = invocation.getArgument(0);
            apiKey.setId(UUID.randomUUID());
            apiKey.setCreatedAt(Instant.parse("2026-02-21T00:00:00Z"));
            return apiKey;
        });

        var response = apiKeyService.createApiKey(new CreateApiKeyRequest(
                "user-1",
                "backend-client"
        ));

        assertEquals("user-1", response.userId());
        assertEquals("backend-client", response.name());
        assertTrue(response.apiKey().startsWith("csk_"));

        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).save(captor.capture());
        ApiKey persisted = captor.getValue();

        // Key format is csk_<prefix>.<secret>; only the hash of pepper.secret is stored.
        String[] keyParts = response.apiKey().split("\\.", 2);
        String prefix = keyParts[0].substring("csk_".length());
        String secret = keyParts[1];

        assertEquals(prefix, persisted.getKeyPrefix());
        assertEquals(sha256Hex("pepper-secret." + secret), persisted.getKeyHash());
    }

    @Test
    void createApiKeyShouldRotateExistingUserKey() {
        ApiKey existing = new ApiKey();
        existing.setId(UUID.randomUUID());
        existing.setUserId("user-1");
        existing.setName("old");
        existing.setKeyPrefix("oldprefix");
        existing.setKeyHash("oldhash");
        existing.setActive(false);

        when(apiKeyRepository.findByUserId("user-1")).thenReturn(Optional.of(existing));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = apiKeyService.createApiKey(new CreateApiKeyRequest("user-1", "new-name"));

        assertEquals("user-1", response.userId());
        assertEquals("new-name", existing.getName());
        assertTrue(existing.isActive());
    }

    @Test
    void authenticateShouldReturnPrincipalWhenKeyMatches() {
        String rawApiKey = "csk_testprefix.secret-value";

        ApiKey stored = new ApiKey();
        stored.setId(UUID.randomUUID());
        stored.setUserId("user-123");
        stored.setKeyPrefix("testprefix");
        stored.setKeyHash(sha256Hex("pepper-secret.secret-value"));
        stored.setActive(true);

        when(apiKeyRepository.findByKeyPrefixAndActiveTrue("testprefix")).thenReturn(Optional.of(stored));

        // Service must validate hash and only return non-sensitive identity context.
        AuthenticatedApiKey authenticated = apiKeyService.authenticate(rawApiKey);

        assertEquals("user-123", authenticated.userId());
        assertEquals("testprefix", authenticated.keyPrefix());
    }

    @Test
    void authenticateShouldRejectInvalidFormat() {
        assertThrows(UnauthorizedException.class, () -> apiKeyService.authenticate("bad-key"));
    }

    @Test
    void authenticateShouldRejectHashMismatch() {
        ApiKey stored = new ApiKey();
        stored.setKeyPrefix("testprefix");
        stored.setKeyHash(sha256Hex("pepper-secret.other"));
        stored.setUserId("user");
        stored.setActive(true);

        when(apiKeyRepository.findByKeyPrefixAndActiveTrue("testprefix")).thenReturn(Optional.of(stored));

        assertThrows(UnauthorizedException.class, () -> apiKeyService.authenticate("csk_testprefix.secret-value"));
    }

    @Test
    void listByUserShouldMapMetadata() {
        ApiKey key = new ApiKey();
        key.setId(UUID.randomUUID());
        key.setUserId("user-1");
        key.setName("key-name");
        key.setKeyPrefix("abc123");
        key.setActive(true);
        key.setCreatedAt(Instant.parse("2026-02-21T00:00:00Z"));

        when(apiKeyRepository.findByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(java.util.List.of(key));

        var metadata = apiKeyService.listByUser("user-1");

        assertEquals(1, metadata.size());
        assertEquals("key-name", metadata.get(0).name());
    }

    @Test
    void revokeShouldDeactivateKey() {
        UUID id = UUID.randomUUID();
        ApiKey key = new ApiKey();
        key.setId(id);
        key.setActive(true);

        when(apiKeyRepository.findById(id)).thenReturn(Optional.of(key));

        apiKeyService.revoke(id);

        assertFalse(key.isActive());
        verify(apiKeyRepository).save(key);
    }

    @Test
    void revokeShouldThrowWhenMissing() {
        UUID id = UUID.randomUUID();
        when(apiKeyRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> apiKeyService.revoke(id));
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
