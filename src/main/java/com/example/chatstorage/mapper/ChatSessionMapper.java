package com.example.chatstorage.mapper;

import com.example.chatstorage.dto.ChatSessionResponse;
import com.example.chatstorage.entity.ChatSession;
import org.springframework.stereotype.Component;

@Component
public class ChatSessionMapper {

    public ChatSessionResponse toResponse(ChatSession session) {
        return new ChatSessionResponse(
                session.getId(),
                session.getUserId(),
                session.getTitle(),
                session.isFavorite(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
