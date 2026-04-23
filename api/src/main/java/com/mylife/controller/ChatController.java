package com.mylife.controller;

import com.mylife.common.BaseResult;
import com.mylife.dto.ChatMessageDTO;
import com.mylife.dto.ChatSendDTO;
import com.mylife.enums.ChatSceneEnum;
import com.mylife.security.SecurityUtils;
import com.mylife.service.IChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final IChatService chatService;

    @PostMapping("/send")
    public SseEmitter send(@Valid @RequestBody ChatSendDTO sendDTO) {
        Long userId = SecurityUtils.getUserId();
        log.info("chat send 请求：userId={}, agentUuid={}, scene={}, message={}",
                userId, sendDTO.getAgentUuid(), sendDTO.getScene(),
                sendDTO.getMessage().length() > 50 ? sendDTO.getMessage().substring(0, 50) + "..." : sendDTO.getMessage());
        return chatService.chat(userId, sendDTO.getAgentUuid(), sendDTO.getMessage(), sendDTO.getScene());
    }

    @PostMapping("/history")
    public BaseResult<List<ChatMessageDTO>> history(@RequestParam String agentUuid,
                                                     @RequestParam(required = false) ChatSceneEnum scene) {
        Long userId = SecurityUtils.getUserId();
        return BaseResult.success(chatService.getHistory(userId, agentUuid, scene));
    }

    @DeleteMapping("/clear")
    public BaseResult<Void> clear(@RequestParam String agentUuid,
                                   @RequestParam(required = false) ChatSceneEnum scene) {
        Long userId = SecurityUtils.getUserId();
        chatService.clearHistory(userId, agentUuid, scene);
        return BaseResult.success(null);
    }
}
