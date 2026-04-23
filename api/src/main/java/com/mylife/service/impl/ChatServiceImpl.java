package com.mylife.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mylife.common.BizException;
import com.mylife.common.ErrorCode;
import com.mylife.dto.ChatMessageDTO;
import com.mylife.entity.ChatMessageDO;
import com.mylife.enums.ChatRoleEnum;
import com.mylife.enums.ChatSceneEnum;
import com.mylife.mapper.ChatMessageMapper;
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
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public SseEmitter chat(Long userId, String agentUuid, String message, ChatSceneEnum scene) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitter.onCompletion(() -> log.info("SSE完成：userId={}, agentUuid={}", userId, agentUuid));
        emitter.onTimeout(() -> log.warn("SSE超时：userId={}, agentUuid={}", userId, agentUuid));

        ChatSceneEnum effectiveScene = scene != null ? scene : ChatSceneEnum.PUBLISHED;
        log.info("chat 开始：userId={}, agentUuid={}, scene={}", userId, agentUuid, effectiveScene);

        TeacherHarness harness = harnessRegistry.getOrCreate(userId, agentUuid, effectiveScene);
        log.info("harness 获取完成：key={}", harness.getHarnessKey());

        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        executor.submit(() -> {
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
        });

        return emitter;
    }

    @Override
    public List<ChatMessageDTO> getHistory(Long userId, String agentUuid, ChatSceneEnum scene) {
        LambdaQueryWrapper<ChatMessageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessageDO::getUserId, userId)
               .eq(ChatMessageDO::getAgentUuid, agentUuid)
               .eq(scene != null, ChatMessageDO::getScene, scene)
               .orderByDesc(ChatMessageDO::getGmtCreated)
               .last("LIMIT " + MAX_HISTORY);
        List<ChatMessageDO> list = chatMessageMapper.selectList(wrapper);
        Collections.reverse(list);
        return list.stream().map(this::convertToDTO).toList();
    }

    @Override
    public void clearHistory(Long userId, String agentUuid, ChatSceneEnum scene) {
        ChatSceneEnum effectiveScene = scene != null ? scene : ChatSceneEnum.PUBLISHED;

        LambdaQueryWrapper<ChatMessageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessageDO::getUserId, userId)
               .eq(ChatMessageDO::getAgentUuid, agentUuid)
               .eq(ChatMessageDO::getScene, effectiveScene);
        chatMessageMapper.delete(wrapper);

        harnessRegistry.remove(userId, agentUuid, effectiveScene);
        log.info("清空聊天记录：userId={}, agentUuid={}, scene={}", userId, agentUuid, effectiveScene);
    }

    private ChatMessageDTO convertToDTO(ChatMessageDO entity) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(entity.getId());
        dto.setRole(entity.getRole());
        dto.setContent(entity.getContent());
        dto.setToolName(entity.getToolName());
        dto.setGmtCreated(entity.getGmtCreated());
        return dto;
    }
}
