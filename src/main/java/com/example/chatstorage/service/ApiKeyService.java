package com.example.chatstorage.service;

import com.example.chatstorage.auth.AuthenticatedApiKey;
import com.example.chatstorage.dto.apikey.ApiKeyMetadataResponse;
import com.example.chatstorage.dto.apikey.CreateApiKeyRequest;
import com.example.chatstorage.dto.apikey.IssueApiKeyResponse;

import java.util.List;
import java.util.UUID;

public interface ApiKeyService {

    IssueApiKeyResponse createApiKey(CreateApiKeyRequest request);

    AuthenticatedApiKey authenticate(String providedApiKey);

    List<ApiKeyMetadataResponse> listByUser(String userId);

    void revoke(UUID apiKeyId);
}
