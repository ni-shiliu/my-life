package com.mylife.service.harness;

import io.agentscope.core.hook.ErrorEvent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.hook.PreActingEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import io.agentscope.core.hook.ReasoningChunkEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;

/**
 * SSE 流式推送 Hook。
 * 在 ReActAgent 循环中拦截关键事件，推送 SSE 给前端，同时打印每轮思考日志。
 */
@Slf4j
public class SseStreamingHook implements Hook {

    private int iterationCount = 0;
    private volatile boolean completed = false;
    private SseEmitter currentEmitter;

    public void setEmitter(SseEmitter emitter) {
        this.currentEmitter = emitter;
        this.iterationCount = 0;
        this.completed = false;
    }

    public void markCompleted() {
        this.completed = true;
    }

    public void clearEmitter() {
        this.currentEmitter = null;
        this.iterationCount = 0;
        this.completed = false;
    }

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreReasoningEvent e) {
            iterationCount++;
            int msgCount = e.getInputMessages() != null ? e.getInputMessages().size() : 0;
            log.info("ReAct第{}轮思考开始 | 消息数={}", iterationCount, msgCount);
        } else if (event instanceof ReasoningChunkEvent e) {
            String chunk = e.getIncrementalChunk().getTextContent();
            if (chunk != null && !chunk.isEmpty() && currentEmitter != null && !completed) {
                SseEventHelper.emitEvent(currentEmitter, "STREAM_CHUNK",
                        SseEventHelper.buildStreamChunkPayload(chunk));
            }
        } else if (event instanceof PreActingEvent e) {
            String toolName = e.getToolUse().getName();
            String toolArgs = e.getToolUse().getInput() != null
                    ? e.getToolUse().getInput().toString() : "";
            log.info("ReAct第{}轮调用工具 | tool={}, args={}", iterationCount, toolName,
                    toolArgs.length() > 200 ? toolArgs.substring(0, 200) + "..." : toolArgs);
            if (currentEmitter != null && !completed) {
                SseEventHelper.emitEvent(currentEmitter, "TOOL_CALL",
                        SseEventHelper.buildToolCallPayload(toolName, toolArgs));
            }
        } else if (event instanceof PostActingEvent e) {
            String toolName = e.getToolUse().getName();
            String result = e.getToolResult() != null ? e.getToolResult().toString() : "";
            log.info("ReAct第{}轮工具结果 | tool={}, result={}", iterationCount, toolName,
                    result.length() > 500 ? result.substring(0, 500) + "..." : result);
        } else if (event instanceof ErrorEvent e) {
            log.error("ReAct第{}轮错误 | error={}", iterationCount, e.getError().getMessage());
            if (currentEmitter != null && !completed) {
                SseEventHelper.emitEvent(currentEmitter, "ERROR",
                        SseEventHelper.buildErrorPayload("LLM_ERROR",
                                e.getError().getMessage(), true));
            }
        }

        return Mono.just(event);
    }
}
