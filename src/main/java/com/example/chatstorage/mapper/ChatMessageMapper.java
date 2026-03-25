package com.example.chatstorage.mapper;

import com.example.chatstorage.dto.ChatMessageResponse;
import com.example.chatstorage.entity.ChatMessage;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageMapper {

    public ChatMessageResponse toResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getSession().getId(),
                message.getSender(),
                message.getContent(),
                message.getRetrievedContext(),
                message.getCreatedAt()
        );
    }
}
