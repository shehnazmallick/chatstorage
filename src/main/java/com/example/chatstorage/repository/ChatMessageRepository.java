package com.example.chatstorage.repository;

import com.example.chatstorage.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    Page<ChatMessage> findBySessionId(UUID sessionId, Pageable pageable);

    void deleteBySessionId(UUID sessionId);
}
