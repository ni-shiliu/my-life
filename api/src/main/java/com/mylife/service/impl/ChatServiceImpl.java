package com.mylife.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mylife.common.BizException;
import com.mylife.common.ErrorCode;
import com.mylife.dto.ChatMessageDTO;
import com.mylife.entity.ChatMessageDO;
import com.mylife.entity.ChatRoomDO;
import com.mylife.enums.ChatRoleEnum;
import com.mylife.enums.ChatSceneEnum;
import com.mylife.mapper.ChatMessageMapper;
import com.mylife.security.JwtClaims;
import com.mylife.security.JwtTokenProvider;
import com.mylife.service.IChatRoomService;
import com.mylife.service.IChatService;
import com.mylife.service.harness.HarnessRegistry;
import com.mylife.service.harness.SseEventHelper;
import com.mylife.service.harness.TeacherHarness;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements IChatService {

    private static final long SSE_TIMEOUT_MS = 3_600_000; // 1 hour
    private static final int MAX_HISTORY = 20;

    private final HarnessRegistry harnessRegistry;
    private final ChatMessageMapper chatMessageMapper;
    private final IChatRoomService chatRoomService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public SseEmitter chat(Long userId, String agentUuid, String message, ChatSceneEnum scene) {
        ChatSceneEnum effectiveScene = scene != null ? scene : ChatSceneEnum.PUBLISHED;
        SseEmitter emitter = buildEmitter("userId=" + userId + ", agentUuid=" + agentUuid);
        log.info("chat 开始：userId={}, agentUuid={}, scene={}", userId, agentUuid, effectiveScene);

        TeacherHarness harness = harnessRegistry.getOrCreate(userId, agentUuid, effectiveScene);
        log.info("harness 获取完成：key={}", harness.getHarnessKey());

        runHarnessAsync(harness, message, emitter);
        return emitter;
    }

    @Override
    public SseEmitter chatGuest(String guestId, String agentUuid, String message) {
        SseEmitter emitter = buildEmitter("guestId=" + guestId + ", agentUuid=" + agentUuid);
        log.info("chatGuest 开始：guestId={}, agentUuid={}", guestId, agentUuid);

        TeacherHarness harness = harnessRegistry.getOrCreateGuest(guestId, agentUuid);
        log.info("访客 harness 获取完成：key={}", harness.getHarnessKey());

        runHarnessAsync(harness, message, emitter);
        return emitter;
    }

    private SseEmitter buildEmitter(String logCtx) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitter.onCompletion(() -> log.info("SSE完成：{}", logCtx));
        emitter.onTimeout(() -> log.warn("SSE超时：{}", logCtx));
        return emitter;
    }

    private void runHarnessAsync(TeacherHarness harness, String message, SseEmitter emitter) {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        executor.submit(() -> dispatchHarness(harness, message, emitter, authentication));
    }

    private void dispatchHarness(TeacherHarness harness, String message, SseEmitter emitter, Authentication authentication) {
        SecurityContext asyncContext = SecurityContextHolder.createEmptyContext();
        asyncContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(asyncContext);
        try {
            log.info("harness.chat 开始执行：key={}", harness.getHarnessKey());
            harness.chat(message, emitter);
            log.info("harness.chat 执行完成：key={}", harness.getHarnessKey());
        } catch (BizException e) {
            log.error("harness.chat 业务异常：key={}, code={}, msg={}", harness.getHarnessKey(), e.getCode(), e.getMessage());
            SseEventHelper.emitEvent(emitter, "ERROR",
                    SseEventHelper.buildErrorPayload(e.getCode(), e.getMessage(), true));
            emitter.complete();
        } catch (Exception e) {
            log.error("harness.chat 系统异常：key={}, error={}", harness.getHarnessKey(), e.getMessage(), e);
            SseEventHelper.emitEvent(emitter, "ERROR",
                    SseEventHelper.buildErrorPayload(ErrorCode.LLM_ERROR.getCode(), "AI服务暂时不可用", true));
            emitter.complete();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    public String ensureRoom(Long userId, String agentUuid, ChatSceneEnum scene) {
        ChatSceneEnum effectiveScene = scene != null ? scene : ChatSceneEnum.PUBLISHED;
        ChatRoomDO room = chatRoomService.getOrCreate(userId, agentUuid, effectiveScene);
        return room.getRoomId();
    }

    @Override
    public List<ChatMessageDTO> getHistory(Long userId, String roomId) {
        ChatRoomDO room = chatRoomService.getAndCheckOwnerByRoomId(userId, roomId);
        LambdaQueryWrapper<ChatMessageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessageDO::getRoomId, room.getId())
               .orderByDesc(ChatMessageDO::getGmtCreated, ChatMessageDO::getId)
               .last("LIMIT " + MAX_HISTORY);
        List<ChatMessageDO> list = chatMessageMapper.selectList(wrapper);
        Collections.reverse(list);
        return list.stream().map(this::convertToDTO).toList();
    }

    @Override
    public void clearHistory(Long userId, String roomId) {
        ChatRoomDO room = chatRoomService.getAndCheckOwnerByRoomId(userId, roomId);
        chatRoomService.clearByRoomId(room.getId());
        harnessRegistry.remove(userId, room.getAgentUuid(), room.getScene());
        log.info("清空聊天记录：userId={}, roomId={}", userId, roomId);
    }

    @Override
    public void clearMemory(Long userId, String roomId) {
        ChatRoomDO room = chatRoomService.getAndCheckOwnerByRoomId(userId, roomId);
        chatRoomService.clearMemoryByRoomId(room.getId());
        harnessRegistry.removeWithoutPersist(userId, room.getAgentUuid(), room.getScene());
        log.info("清空上下文记忆：userId={}, roomId={}", userId, roomId);
    }

    private ChatMessageDTO convertToDTO(ChatMessageDO entity) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(entity.getId());
        dto.setMessageId(entity.getMessageId());
        dto.setRoomId(entity.getRoomId());
        dto.setRole(entity.getRole());
        dto.setContent(entity.getContent());
        dto.setToolName(entity.getToolName());
        dto.setGmtCreated(entity.getGmtCreated());
        return dto;
    }

    @Override
    public HarnessRegistry.ClaimResult claimGuestHistory(Long userId, String guestToken) {
        JwtClaims claims = jwtTokenProvider.validateAllowExpired(guestToken);
        if (!"guest".equals(claims.getType()) || claims.getGuestId() == null) {
            throw new BizException(ErrorCode.TOKEN_INVALID.getCode(), "无效的访客凭证");
        }
        return harnessRegistry.claimGuestHarnesses(claims.getGuestId(), userId);
    }
}
