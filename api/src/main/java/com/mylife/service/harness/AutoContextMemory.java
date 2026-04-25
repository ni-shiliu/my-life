package com.mylife.service.harness;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mylife.entity.ContextMemoryDO;
import com.mylife.mapper.ContextMemoryMapper;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatModelBase;
import io.agentscope.core.model.ChatResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动上下文压缩记忆引擎。
 * 实现 SDK Memory 接口，自行管理消息列表，增加 auto_compact / reminder 逻辑。
 * micro_compact 和 reminder 注入已迁移到 ContextCompressionHook。
 * 非 Spring Bean，每个 TeacherHarness 持有一个实例。
 */
@Slf4j
public class AutoContextMemory implements Memory {

    private static final int MAX_MESSAGES = 4;
    private static final int KEEP_RECENT = 2;

    private final List<Msg> messages = new ArrayList<>();
    private final ChatModelBase compactModel;
    private final ContextMemoryMapper contextMemoryMapper;
    private final Long roomId;
    private final Long userId;
    private final String agentId;

    private String persistedMemory;

    public AutoContextMemory(ChatModelBase compactModel,
                             ContextMemoryMapper contextMemoryMapper,
                             Long roomId,
                             Long userId,
                             String agentId) {
        this.compactModel = compactModel;
        this.contextMemoryMapper = contextMemoryMapper;
        this.roomId = roomId;
        this.userId = userId;
        this.agentId = agentId;
    }

    /**
     * 初始化系统提示词，作为消息列表的第一条。
     * 在 loadHistory 和 restoreMemory 之前调用，确保 sysPrompt 始终在最前。
     */
    public void init(String sysPrompt) {
        if (sysPrompt != null && !sysPrompt.isBlank()) {
            Msg sysMsg = Msg.builder()
                    .role(MsgRole.SYSTEM)
                    .textContent(sysPrompt)
                    .build();
            messages.add(sysMsg);
        }
    }

    @Override
    public void addMessage(Msg message) {
        messages.add(message);
    }

    /**
     * @deprecated 使用 {@link #addMessage(Msg)} 代替，兼容 SDK Memory 接口
     */
    public void add(Msg message) {
        addMessage(message);
    }

    @Override
    public List<Msg> getMessages() {
        return messages;
    }

    @Override
    public void deleteMessage(int index) {
        messages.remove(index);
    }

    @Override
    public void clear() {
        messages.clear();
    }

    public boolean needsCompact() {
        return messages.size() > MAX_MESSAGES;
    }

    /**
     * 从DB恢复已有记忆。
     */
    public void restoreMemory(String existingMemory) {
        if (existingMemory != null && !existingMemory.isBlank()) {
            this.persistedMemory = existingMemory;
            Msg memoryMsg = Msg.builder()
                    .role(MsgRole.SYSTEM)
                    .textContent("[对话记忆] " + existingMemory)
                    .build();
            messages.add(memoryMsg);
        }
    }

    /**
     * auto_compact：消息超阈值时保留最近N条 + LLM摘要，摘要持久化到DB。
     * micro_compact 和 reminder 注入已迁移到 ContextCompressionHook。
     */
    public void compact() {
        if (messages.size() <= MAX_MESSAGES) {
            return;
        }

        int compactCount = messages.size() - KEEP_RECENT;
        StringBuilder historyText = new StringBuilder();
        for (int i = 0; i < compactCount; i++) {
            Msg msg = messages.get(i);
            historyText.append(msg.getRole().name()).append(": ").append(msg.getTextContent()).append("\n");
        }

        String summary = generateMemory(historyText.toString());
        if (summary == null || summary.isBlank()) {
            log.warn("auto_compact生成记忆失败，跳过压缩：roomId={}", roomId);
            return;
        }

        // 保留最近 KEEP_RECENT 条
        List<Msg> remaining = new ArrayList<>(messages.subList(compactCount, messages.size()));

        // 头部插入摘要
        Msg memoryMsg = Msg.builder()
                .role(MsgRole.SYSTEM)
                .textContent("[对话记忆] " + summary)
                .build();
        remaining.add(0, memoryMsg);

        messages.clear();
        messages.addAll(remaining);
        persistedMemory = summary;

        persistMemoryToDb(summary, compactCount);

        log.info("auto_compact完成：roomId={}, 压缩{}条消息", roomId, compactCount);
    }

    public String getPersistedMemory() {
        return persistedMemory;
    }

    /**
     * 回收前持久化：如果还有未压缩的消息，生成最终记忆。
     */
    public void persistBeforeRecycle() {
        if (messages.isEmpty()) {
            return;
        }

        if (persistedMemory != null && !persistedMemory.isBlank()) {
            return;
        }

        StringBuilder historyText = new StringBuilder();
        for (Msg msg : messages) {
            historyText.append(msg.getRole().name()).append(": ").append(msg.getTextContent()).append("\n");
        }

        String summary = generateMemory(historyText.toString());
        if (summary != null && !summary.isBlank()) {
            persistMemoryToDb(summary, messages.size());
            log.info("回收前持久化记忆：roomId={}, 消息数={}", roomId, messages.size());
        }
    }

    private String generateMemory(String historyText) {
        try {
            String prompt = "请总结以下对话历史，重点包括：已讨论的知识点、学生掌握情况、当前教学方向。" +
                    "请用简洁的中文总结，不超过300字。\n\n" + historyText;

            Msg compactMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .textContent(prompt)
                    .build();

            Msg sysMsg = Msg.builder()
                    .role(MsgRole.SYSTEM)
                    .textContent("你是一个对话摘要助手，请简洁准确地总结对话内容。")
                    .build();

            List<ChatResponse> chunks = compactModel.stream(
                    List.of(sysMsg, compactMsg), null, null).collectList().block();

            if (chunks == null || chunks.isEmpty()) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            for (ChatResponse chunk : chunks) {
                if (chunk.getContent() != null) {
                    for (var block : chunk.getContent()) {
                        if (block instanceof TextBlock) {
                            TextBlock textBlock = (TextBlock) block;
                            sb.append(textBlock.getText());
                        }
                    }
                }
            }
            return sb.toString().isBlank() ? null : sb.toString();
        } catch (Exception e) {
            log.error("生成上下文记忆失败：roomId={}, error={}", roomId, e.getMessage());
            return null;
        }
    }

    private void persistMemoryToDb(String content, int messageCount) {
        ContextMemoryDO memoryDO = new ContextMemoryDO();
        memoryDO.setRoomId(roomId);
        memoryDO.setUserId(userId);
        memoryDO.setAgentId(agentId);
        memoryDO.setContent(content);
        memoryDO.setMessageCount(messageCount);
        contextMemoryMapper.insert(memoryDO);
    }

    /**
     * 从DB加载最新的上下文记忆。
     */
    public static String loadLatestMemory(ContextMemoryMapper mapper, Long roomId) {
        LambdaQueryWrapper<ContextMemoryDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContextMemoryDO::getRoomId, roomId)
               .orderByDesc(ContextMemoryDO::getGmtCreated)
               .last("LIMIT 1");
        ContextMemoryDO memoryDO = mapper.selectOne(wrapper);
        return memoryDO != null ? memoryDO.getContent() : null;
    }
}
