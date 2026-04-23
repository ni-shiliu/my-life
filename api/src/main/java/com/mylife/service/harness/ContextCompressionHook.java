package com.mylife.service.harness;

import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ToolResultBlock;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 上下文压缩与计划追踪 Hook。
 * <ul>
 *   <li>PreReasoningEvent: micro_compact（替换旧工具结果为占位符）+ reminder 注入</li>
 *   <li>PostActingEvent: plan tracking（统计工具调用轮次，决定是否催促）</li>
 * </ul>
 */
@Slf4j
public class ContextCompressionHook implements Hook {

    private static final int MICRO_COMPACT_KEEP_RECENT_TOOLS = 2;
    private static final int REMINDER_THRESHOLD = 3;

    private final AutoContextMemory memory;
    private int roundsSincePlanAdvance;

    public ContextCompressionHook(AutoContextMemory memory) {
        this.memory = memory;
        this.roundsSincePlanAdvance = 0;
    }

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreReasoningEvent e) {
            microCompact(e);
            autoCompactIfNeeded();
            injectReminderIfNeeded(e);
        } else if (event instanceof PostActingEvent e) {
            trackPlanAdvance(e);
        }
        return Mono.just(event);
    }

    /**
     * micro_compact：替换旧工具结果为占位符，保留最近2轮工具结果。
     */
    private void microCompact(PreReasoningEvent event) {
        List<Msg> messages = event.getInputMessages();
        if (messages == null || messages.isEmpty()) {
            return;
        }

        int keepFromEnd = 0;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Msg msg = messages.get(i);
            if (msg.getRole() == MsgRole.TOOL) {
                keepFromEnd++;
                if (keepFromEnd > MICRO_COMPACT_KEEP_RECENT_TOOLS) {
                    String toolName = extractToolName(msg);
                    String placeholder = "[" + toolName + "结果已压缩]";
                    messages.set(i, Msg.builder()
                            .role(MsgRole.TOOL)
                            .textContent(placeholder)
                            .build());
                }
            }
        }
    }

    /**
     * auto_compact：消息超阈值时保留最近N条 + LLM摘要，摘要持久化到DB。
     */
    private void autoCompactIfNeeded() {
        if (memory.needsCompact()) {
            memory.compact();
        }
    }

    /**
     * 连续N轮未调用 advance/finish 工具时注入催促提醒。
     */
    private void injectReminderIfNeeded(PreReasoningEvent event) {
        if (roundsSincePlanAdvance < REMINDER_THRESHOLD) {
            return;
        }

        Msg reminderMsg = Msg.builder()
                .role(MsgRole.SYSTEM)
                .textContent("<reminder>请推进教学计划或完成当前子任务</reminder>")
                .build();
        event.getInputMessages().add(reminderMsg);
        roundsSincePlanAdvance = 0;
        log.info("注入催促提醒");
    }

    /**
     * 追踪计划推进：advance/finish 工具重置计数器，其他工具递增。
     */
    private void trackPlanAdvance(PostActingEvent event) {
        String toolName = event.getToolUse().getName();
        if (toolName.startsWith("advance") || toolName.startsWith("finish")) {
            roundsSincePlanAdvance = 0;
        } else {
            roundsSincePlanAdvance++;
        }
    }

    private String extractToolName(Msg msg) {
        String text = msg.getTextContent();
        if (text != null && text.startsWith("[") && text.contains("结果已压缩]")) {
            return text.substring(1, text.indexOf("结果已压缩]"));
        }
        List<ToolResultBlock> blocks = msg.getContentBlocks(ToolResultBlock.class);
        if (!blocks.isEmpty()) {
            return blocks.get(0).getName();
        }
        return "tool";
    }
}
