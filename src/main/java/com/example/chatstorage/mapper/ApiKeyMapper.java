package com.example.chatstorage.mapper;

import com.example.chatstorage.dto.apikey.ApiKeyMetadataResponse;
import com.example.chatstorage.dto.apikey.IssueApiKeyResponse;
import com.example.chatstorage.entity.ApiKey;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyMapper {

    public ApiKeyMetadataResponse toMetadata(ApiKey apiKey) {
        return new ApiKeyMetadataResponse(
                apiKey.getId(),
                apiKey.getUserId(),
                apiKey.getName(),
                apiKey.getKeyPrefix(),
                apiKey.isActive(),
                apiKey.getCreatedAt(),
                apiKey.getLastUsedAt()
        );
    }

    public IssueApiKeyResponse toIssueResponse(ApiKey apiKey, String rawApiKey) {
        return new IssueApiKeyResponse(
                apiKey.getId(),
                apiKey.getUserId(),
                apiKey.getName(),
                rawApiKey,
                apiKey.getCreatedAt()
        );
    }
}
