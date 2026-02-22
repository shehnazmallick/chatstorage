package com.example.chatstorage.repository;

import com.example.chatstorage.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    Page<ChatSession> findByUserId(String userId, Pageable pageable);

    Page<ChatSession> findByUserIdAndFavorite(String userId, boolean favorite, Pageable pageable);

    Optional<ChatSession> findByIdAndUserId(UUID id, String userId);
}
