package com.mylife.service.harness;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE事件构建与发送工具类。
 */
@Slf4j
public final class SseEventHelper {

    private SseEventHelper() {}

    public static void emitEvent(SseEmitter emitter, String type, JSONObject payload) {
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", type);
            msg.put("payload", payload);
            emitter.send(SseEmitter.event().data(msg.toJSONString()));
        } catch (Exception e) {
            log.warn("发送SSE事件失败：type={}, error={}", type, e.getMessage());
        }
    }

    public static JSONObject buildStreamChunkPayload(String chunk) {
        JSONObject payload = new JSONObject();
        payload.put("chunk", chunk);
        return payload;
    }

    public static JSONObject buildToolCallPayload(String toolName, String toolArgs) {
        JSONObject payload = new JSONObject();
        payload.put("toolName", toolName);
        payload.put("toolArgs", toolArgs);
        return payload;
    }

    public static JSONObject buildStreamEndPayload(String fullText) {
        JSONObject payload = new JSONObject();
        payload.put("fullText", fullText);
        return payload;
    }

    public static JSONObject buildErrorPayload(String code, String message, boolean retryable) {
        JSONObject payload = new JSONObject();
        payload.put("code", code);
        payload.put("message", message);
        payload.put("retryable", retryable);
        return payload;
    }
}
