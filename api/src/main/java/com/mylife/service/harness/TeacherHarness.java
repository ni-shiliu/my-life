package com.mylife.service.harness;

import com.mylife.common.BizException;
import com.mylife.common.ErrorCode;
import com.mylife.entity.ChatMessageDO;
import com.mylife.enums.ChatRoleEnum;
import com.mylife.enums.ChatSceneEnum;
import com.mylife.mapper.ChatMessageMapper;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 每个用户会话的 Harness 实例。
 * 委托 ReActAgent 驱动推理+工具循环，通过 Hook 做监控控制。
 * Harness 负责：SseEmitter 生命周期、DB 持久化、并发锁。
 */
@Slf4j
public class TeacherHarness {

    private final String harnessKey;
    private final Long userId;
    private final String agentUuid;
    private final ChatSceneEnum scene;
    private final Long roomId;
    private final ReActAgent agent;
    private final AutoContextMemory memory;
    private final ChatMessageMapper chatMessageMapper;
    private final SseStreamingHook sseHook;
    private final ContextCompressionHook compressionHook;
    private final ReentrantLock lock;
    private volatile Instant lastActiveAt;

    public TeacherHarness(String harnessKey,
                          Long userId,
                          String agentUuid,
                          ChatSceneEnum scene,
                          Long roomId,
                          ReActAgent agent,
                          AutoContextMemory memory,
                          ChatMessageMapper chatMessageMapper,
                          SseStreamingHook sseHook,
                          ContextCompressionHook compressionHook) {
        this.harnessKey = harnessKey;
        this.userId = userId;
        this.agentUuid = agentUuid;
        this.scene = scene;
        this.roomId = roomId;
        this.agent = agent;
        this.memory = memory;
        this.chatMessageMapper = chatMessageMapper;
        this.sseHook = sseHook;
        this.compressionHook = compressionHook;
        this.lock = new ReentrantLock();
        this.lastActiveAt = Instant.now();
    }

    /**
     * 主入口：加锁 → 保存用户消息 → ReActAgent 循环 → 保存助手回复 → 推送 SSE。
     */
    public void chat(String message, SseEmitter emitter) {
        lock.lock();
        try {
            lastActiveAt = Instant.now();
            sseHook.setEmitter(emitter);

            if (userId != null) {
                saveMessage(ChatRoleEnum.USER, message, null);
            }

            Msg userMsg = Msg.builder().role(MsgRole.USER).textContent(message).build();
            Msg response = agent.call(userMsg).block();

            String finalAnswer = extractTextContent(response);

            if (userId != null && finalAnswer != null && !finalAnswer.isBlank()) {
                saveMessage(ChatRoleEnum.ASSISTANT, finalAnswer, null);
            }

            SseEventHelper.emitEvent(emitter, "STREAM_END",
                    SseEventHelper.buildStreamEndPayload(finalAnswer != null ? finalAnswer : ""));
            sseHook.markCompleted();
            emitter.complete();
        } catch (BizException e) {
            sseHook.markCompleted();
            SseEventHelper.emitEvent(emitter, "ERROR",
                    SseEventHelper.buildErrorPayload(e.getCode(), e.getMessage(), true));
            emitter.complete();
        } catch (Exception e) {
            log.error("聊天处理异常：harnessKey={}, error={}", harnessKey, e.getMessage(), e);
            sseHook.markCompleted();
            SseEventHelper.emitEvent(emitter, "ERROR",
                    SseEventHelper.buildErrorPayload(ErrorCode.LLM_ERROR.getCode(), "AI服务暂时不可用", true));
            emitter.complete();
        } finally {
            sseHook.clearEmitter();
            lock.unlock();
        }
    }

    private String extractTextContent(Msg response) {
        if (response == null) {
            return "";
        }
        String text = response.getTextContent();
        return text != null ? text : "";
    }

    /**
     * 回收前持久化记忆到DB。
     */
    public void persistBeforeRecycle() {
        memory.persistBeforeRecycle();
        log.info("Harness回收持久化：key={}", harnessKey);
    }

    public boolean isIdle(long thresholdMs) {
        return Duration.between(lastActiveAt, Instant.now()).toMillis() > thresholdMs;
    }

    public boolean isLocked() {
        return lock.isLocked();
    }

    public String getHarnessKey() {
        return harnessKey;
    }

    private void saveMessage(ChatRoleEnum role, String content, String toolName) {
        ChatMessageDO msg = new ChatMessageDO();
        msg.setMessageId(java.util.UUID.randomUUID().toString());
        msg.setRoomId(roomId);
        msg.setUserId(userId);
        msg.setAgentUuid(agentUuid);
        msg.setRole(role);
        msg.setContent(content);
        msg.setToolName(toolName);
        msg.setScene(scene);
        chatMessageMapper.insert(msg);
    }
}
