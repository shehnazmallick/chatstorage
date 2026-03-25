package com.example.chatstorage.service.impl;

import com.example.chatstorage.auth.AuthenticatedApiKey;
import com.example.chatstorage.config.AppProperties;
import com.example.chatstorage.dto.apikey.ApiKeyMetadataResponse;
import com.example.chatstorage.dto.apikey.CreateApiKeyRequest;
import com.example.chatstorage.dto.apikey.IssueApiKeyResponse;
import com.example.chatstorage.entity.ApiKey;
import com.example.chatstorage.exception.NotFoundException;
import com.example.chatstorage.exception.UnauthorizedException;
import com.example.chatstorage.mapper.ApiKeyMapper;
import com.example.chatstorage.repository.ApiKeyRepository;
import com.example.chatstorage.service.ApiKeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ApiKeyServiceImpl implements ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String KEY_PREFIX_LABEL = "csk";

    private final ApiKeyRepository apiKeyRepository;
    private final AppProperties appProperties;
    private final ApiKeyMapper apiKeyMapper;

    public ApiKeyServiceImpl(ApiKeyRepository apiKeyRepository,
                             AppProperties appProperties,
                             ApiKeyMapper apiKeyMapper) {
        this.apiKeyRepository = apiKeyRepository;
        this.appProperties = appProperties;
        this.apiKeyMapper = apiKeyMapper;
    }

    @Override
    @Transactional
    public IssueApiKeyResponse createApiKey(CreateApiKeyRequest request) {
        String secret = generateSecret();
        String publicPrefix = generatePublicPrefix();
        String userId = request.userId().trim();

        ApiKey apiKey = apiKeyRepository.findByUserId(userId).orElseGet(ApiKey::new);
        apiKey.setUserId(userId);
        apiKey.setName(request.name().trim());
        apiKey.setKeyPrefix(publicPrefix);
        apiKey.setKeyHash(hashSecret(secret));
        apiKey.setActive(true);

        ApiKey saved = apiKeyRepository.save(apiKey);
        log.info("Issued API key for userId={} apiKeyId={}", saved.getUserId(), saved.getId());

        return apiKeyMapper.toIssueResponse(saved, formatApiKey(publicPrefix, secret));
    }

    @Override
    @Transactional
    public AuthenticatedApiKey authenticate(String providedApiKey) {
        ParsedApiKey parsed = parseApiKey(providedApiKey);
        ApiKey apiKey = apiKeyRepository.findByKeyPrefixAndActiveTrue(parsed.prefix())
                .orElseThrow(() -> new UnauthorizedException("Invalid API key"));

        String expectedHash = apiKey.getKeyHash();
        String actualHash = hashSecret(parsed.secret());
        if (!constantTimeEquals(expectedHash, actualHash)) {
            log.warn("Rejected API key authentication for prefix={}", parsed.prefix());
            throw new UnauthorizedException("Invalid API key");
        }

        apiKey.setLastUsedAt(java.time.Instant.now());
        apiKeyRepository.save(apiKey);
        log.debug("Authenticated API key for userId={} apiKeyId={}", apiKey.getUserId(), apiKey.getId());

        return new AuthenticatedApiKey(
                apiKey.getId(),
                apiKey.getUserId(),
                apiKey.getKeyPrefix()
        );
    }

    @Override
    public List<ApiKeyMetadataResponse> listByUser(String userId) {
        String normalizedUserId = userId.trim();
        log.debug("Listing API keys for userId={}", normalizedUserId);
        return apiKeyRepository.findByUserIdOrderByCreatedAtDesc(normalizedUserId).stream()
                .map(apiKeyMapper::toMetadata)
                .toList();
    }

    @Override
    @Transactional
    public void revoke(UUID apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new NotFoundException("API key not found: " + apiKeyId));
        apiKey.setActive(false);
        apiKeyRepository.save(apiKey);
        log.info("Revoked API key apiKeyId={} userId={}", apiKey.getId(), apiKey.getUserId());
    }

    private String formatApiKey(String prefix, String secret) {
        return KEY_PREFIX_LABEL + "_" + prefix + "." + secret;
    }

    private ParsedApiKey parseApiKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new UnauthorizedException("Missing API key");
        }

        int dotIndex = rawKey.indexOf('.');
        if (dotIndex <= 0 || dotIndex >= rawKey.length() - 1) {
            throw new UnauthorizedException("Invalid API key format");
        }

        String prefixPart = rawKey.substring(0, dotIndex);
        String secretPart = rawKey.substring(dotIndex + 1);
        String expectedPrefix = KEY_PREFIX_LABEL + "_";
        if (!prefixPart.startsWith(expectedPrefix)) {
            throw new UnauthorizedException("Invalid API key format");
        }

        String prefix = prefixPart.substring(expectedPrefix.length());
        if (prefix.isBlank() || secretPart.isBlank()) {
            throw new UnauthorizedException("Invalid API key format");
        }

        return new ParsedApiKey(prefix, secretPart);
    }

    private String generatePublicPrefix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String generateSecret() {
        byte[] random = new byte[32];
        SECURE_RANDOM.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private String hashSecret(String secret) {
        String pepper = appProperties.getSecurity().getApiKeyPepper();
        if (pepper == null || pepper.isBlank()) {
            throw new UnauthorizedException("API key pepper is not configured");
        }

        return sha256Hex(pepper + "." + secret);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }

    private record ParsedApiKey(String prefix, String secret) {
    }
}
