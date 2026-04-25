package com.mylife.service;

import com.mylife.dto.ChatMessageDTO;
import com.mylife.enums.ChatSceneEnum;

import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface IChatService {

    SseEmitter chat(Long userId, String agentUuid, String message, ChatSceneEnum scene);

    String ensureRoom(Long userId, String agentUuid, ChatSceneEnum scene);

    List<ChatMessageDTO> getHistory(Long userId, String roomId);

    void clearHistory(Long userId, String roomId);

    void clearMemory(Long userId, String roomId);
}
