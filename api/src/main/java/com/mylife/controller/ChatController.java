package com.mylife.controller;

import com.mylife.common.BaseResult;
import com.mylife.common.BizException;
import com.mylife.common.ErrorCode;
import com.mylife.dto.ChatClaimGuestDTO;
import com.mylife.dto.ChatMessageDTO;
import com.mylife.dto.ChatSendDTO;
import com.mylife.enums.ChatSceneEnum;
import com.mylife.security.SecurityUtils;
import com.mylife.service.IChatService;
import com.mylife.service.harness.HarnessRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final IChatService chatService;

    @PostMapping("/send")
    public SseEmitter send(@Valid @RequestBody ChatSendDTO sendDTO) {
        if (SecurityUtils.isGuest()) {
            return sendAsGuest(sendDTO);
        }
        return sendAsUser(sendDTO);
    }

    private SseEmitter sendAsUser(ChatSendDTO sendDTO) {
        Long userId = SecurityUtils.getUserId();
        log.info("chat send 请求：userId={}, agentUuid={}, scene={}, messagePreview={}",
                userId, sendDTO.getAgentUuid(), sendDTO.getScene(), preview(sendDTO.getMessage()));
        return chatService.chat(userId, sendDTO.getAgentUuid(), sendDTO.getMessage(), sendDTO.getScene());
    }

    private SseEmitter sendAsGuest(ChatSendDTO sendDTO) {
        ChatSceneEnum scene = sendDTO.getScene();
        if (scene != null && scene != ChatSceneEnum.PUBLISHED) {
            throw new BizException(ErrorCode.PARAM_ILLEGAL.getCode(), "访客仅支持已发布场景");
        }
        String guestId = SecurityUtils.getOptionalGuestId()
                .orElseThrow(() -> new BizException(ErrorCode.TOKEN_INVALID.getCode(), "访客身份无效"));
        log.info("chat send 访客请求：guestId={}, agentUuid={}, messagePreview={}",
                guestId, sendDTO.getAgentUuid(), preview(sendDTO.getMessage()));
        return chatService.chatGuest(guestId, sendDTO.getAgentUuid(), sendDTO.getMessage());
    }

    private String preview(String message) {
        if (message == null) {
            return "";
        }
        return message.length() > 50 ? message.substring(0, 50) + "..." : message;
    }

    @PostMapping("/room")
    public BaseResult<Map<String, Object>> ensureRoom(@RequestParam String agentUuid,
                                                       @RequestParam(required = false) ChatSceneEnum scene) {
        Long userId = SecurityUtils.getUserId();
        String roomId = chatService.ensureRoom(userId, agentUuid, scene);
        return BaseResult.success(Map.of("roomId", roomId));
    }

    @PostMapping("/history")
    public BaseResult<List<ChatMessageDTO>> history(@RequestParam String roomId) {
        Long userId = SecurityUtils.getUserId();
        return BaseResult.success(chatService.getHistory(userId, roomId));
    }

    @DeleteMapping("/clear")
    public BaseResult<Void> clear(@RequestParam String roomId) {
        Long userId = SecurityUtils.getUserId();
        chatService.clearHistory(userId, roomId);
        return BaseResult.success(null);
    }

    @DeleteMapping("/clear-memory")
    public BaseResult<Void> clearMemory(@RequestParam String roomId) {
        Long userId = SecurityUtils.getUserId();
        chatService.clearMemory(userId, roomId);
        return BaseResult.success(null);
    }

    @PostMapping("/claim-guest")
    public BaseResult<Map<String, Object>> claimGuest(@Valid @RequestBody ChatClaimGuestDTO claimDTO) {
        if (SecurityUtils.isGuest()) {
            throw new BizException(ErrorCode.PARAM_ILLEGAL.getCode(), "访客身份不能调用归并接口");
        }
        Long userId = SecurityUtils.getUserId();
        HarnessRegistry.ClaimResult result = chatService.claimGuestHistory(userId, claimDTO.getGuestToken());
        log.info("访客归并接口完成：userId={}, count={}, agents={}",
                userId, result.messageCount(), result.agentUuids());
        return BaseResult.success(Map.of(
                "count", result.messageCount(),
                "agentUuids", result.agentUuids()));
    }
}
