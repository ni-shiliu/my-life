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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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
    private volatile boolean claimed;

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
            if (claimed) {
                SseEventHelper.emitEvent(emitter, "ERROR",
                        SseEventHelper.buildErrorPayload("GUEST_CLAIMED",
                                "访客会话已归并到您的账号，请刷新页面继续对话", false));
                emitter.complete();
                return;
            }
            lastActiveAt = Instant.now();
            sseHook.setEmitter(emitter);

            saveMessage(ChatRoleEnum.USER, message, null);

            Msg userMsg = Msg.builder().role(MsgRole.USER).textContent(message).build();
            Msg response = agent.call(userMsg).block();

            String finalAnswer = extractTextContent(response);

            if (finalAnswer != null && !finalAnswer.isBlank()) {
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
        if (roomId == null) {
            log.info("访客态跳过回收持久化：key={}", harnessKey);
            return;
        }
        memory.persistBeforeRecycle();
        log.info("Harness回收持久化：key={}", harnessKey);
    }

    /**
     * 抢占式归并：先 tryLock 限时拿锁，成功则标记 claimed 并复制内存快照后释放锁。
     * 调用方应已在调用前从 HarnessRegistry.harnessMap 摘除本实例，避免新请求拿到。
     */
    public Optional<ClaimSnapshot> tryClaim() {
        boolean acquired;
        try {
            acquired = lock.tryLock(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
        if (!acquired) {
            return Optional.empty();
        }
        try {
            claimed = true;
            List<Msg> snapshot = new ArrayList<>(memory.getMessages());
            return Optional.of(new ClaimSnapshot(snapshot, memory.getPersistedMemory()));
        } finally {
            lock.unlock();
        }
    }

    public record ClaimSnapshot(List<Msg> messages, String persistedMemory) {}

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
        if (roomId == null) {
            return;
        }
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
