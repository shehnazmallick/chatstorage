package com.example.chatstorage.repository;

import com.example.chatstorage.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Optional<ApiKey> findByKeyPrefixAndActiveTrue(String keyPrefix);

    List<ApiKey> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<ApiKey> findByUserId(String userId);
}
