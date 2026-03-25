package com.example.chatstorage.service;

import com.example.chatstorage.dto.AddMessageRequest;
import com.example.chatstorage.dto.ChatMessageResponse;
import com.example.chatstorage.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ChatMessageService {

    ChatMessageResponse addMessage(UUID sessionId, String userId, AddMessageRequest request);

    PageResponse<ChatMessageResponse> listMessages(UUID sessionId, String userId, Pageable pageable);
}
