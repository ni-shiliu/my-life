package com.mylife.service.harness;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mylife.common.BizException;
import com.mylife.common.ErrorCode;
import com.mylife.entity.AgentDO;
import com.mylife.entity.ChatRoomDO;
import com.mylife.entity.ContextMemoryDO;
import com.mylife.entity.KnowledgeBaseDO;
import com.mylife.entity.ChatMessageDO;
import com.mylife.enums.AgentStatusEnum;
import com.mylife.enums.ChatRoleEnum;
import com.mylife.enums.ChatSceneEnum;
import com.mylife.mapper.AgentMapper;
import com.mylife.mapper.ChatMessageMapper;
import com.mylife.mapper.ContextMemoryMapper;
import com.mylife.mapper.KnowledgeBaseMapper;
import com.mylife.service.IChatRoomService;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.rag.RAGMode;
import io.agentscope.core.rag.integration.bailian.BailianConfig;
import io.agentscope.core.rag.integration.bailian.BailianKnowledge;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.tool.ToolExecutionContext;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Harness 生命周期管理器。
 * 按 userId_agentUuid 管理 TeacherHarness 实例的创建/复用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HarnessRegistry {

    private static final int MAX_HISTORY = 20;

    private static final String GUEST_KEY_PREFIX = "g:";

    private final ConcurrentHashMap<String, TeacherHarness> harnessMap = new ConcurrentHashMap<>();

    private final ChatMessageMapper chatMessageMapper;
    private final AgentMapper agentMapper;
    private final ContextMemoryMapper contextMemoryMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final IChatRoomService chatRoomService;

//    @Value("${llm.base-url}")
//    private String baseUrl;

    @Value("${llm.api-key}")
    private String apiKey;

    @Value("${llm.model:qwen3-max}")
    private String modelName;

    @Value("${llm.max-iterations:10}")
    private int maxIterations;

    @Value("${bailian.access-key-id:}")
    private String bailianAccessKeyId;

    @Value("${bailian.access-key-secret:}")
    private String bailianAccessKeySecret;

    @Value("${bailian.workspace-id:}")
    private String bailianWorkspaceId;

    /**
     * 获取或创建 TeacherHarness。线程安全（computeIfAbsent）。
     */
    public TeacherHarness getOrCreate(Long userId, String agentUuid, ChatSceneEnum scene) {
        String key = buildKey(userId, agentUuid, scene);
        return harnessMap.computeIfAbsent(key, k -> {
            log.info("创建TeacherHarness：key={}", k);
            return createHarness(k, userId, agentUuid, scene);
        });
    }

    /**
     * 销毁并移除 TeacherHarness 实例。
     */
    public void remove(Long userId, String agentUuid, ChatSceneEnum scene) {
        String key = buildKey(userId, agentUuid, scene);
        TeacherHarness harness = harnessMap.remove(key);
        if (harness != null) {
            harness.persistBeforeRecycle();
            log.info("销毁TeacherHarness：key={}", key);
        }
    }

    /**
     * 销毁并移除 TeacherHarness 实例，不持久化记忆（用于清空记忆场景）。
     */
    public void removeWithoutPersist(Long userId, String agentUuid, ChatSceneEnum scene) {
        String key = buildKey(userId, agentUuid, scene);
        TeacherHarness harness = harnessMap.remove(key);
        if (harness != null) {
            log.info("销毁TeacherHarness（不持久化记忆）：key={}", key);
        }
    }

    /**
     * 获取或创建访客 TeacherHarness。线程安全（computeIfAbsent）。
     * 仅支持 PUBLISHED 场景，不持久化。
     */
    public TeacherHarness getOrCreateGuest(String guestId, String agentUuid) {
        String key = buildGuestKey(guestId, agentUuid);
        return harnessMap.computeIfAbsent(key, k -> {
            log.info("创建访客TeacherHarness：key={}", k);
            return createGuestHarness(k, guestId, agentUuid);
        });
    }

    /**
     * 销毁并移除访客 TeacherHarness 实例（不持久化记忆，访客态本就不落库）。
     */
    public void removeGuest(String guestId, String agentUuid) {
        String key = buildGuestKey(guestId, agentUuid);
        TeacherHarness harness = harnessMap.remove(key);
        if (harness != null) {
            log.info("销毁访客TeacherHarness：key={}", key);
        }
    }

    private String buildKey(Long userId, String agentUuid, ChatSceneEnum scene) {
        return userId + "_" + agentUuid + "_" + scene.name();
    }

    private String buildGuestKey(String guestId, String agentUuid) {
        return GUEST_KEY_PREFIX + guestId + "_" + agentUuid + "_" + ChatSceneEnum.PUBLISHED.name();
    }

    private TeacherHarness createHarness(String key, Long userId, String agentUuid, ChatSceneEnum scene) {
        AgentDO agent = loadAndValidateAgent(agentUuid);
        ChatRoomDO room = chatRoomService.getOrCreate(userId, agentUuid, scene);

        DashScopeChatModel model = buildStreamingModel();
        DashScopeChatModel compactModel = buildCompactModel();

        AutoContextMemory memory = new AutoContextMemory(
                compactModel, contextMemoryMapper, room.getId(), userId, agentUuid);

        // sysPrompt 由 ReActAgent.builder().sysPrompt() 管理，不再手动注入 memory
        loadHistoryToMemory(userId, agentUuid, memory);
        String existingMemory = AutoContextMemory.loadLatestMemory(contextMemoryMapper, room.getId());
        memory.restoreMemory(existingMemory);

        Toolkit toolkit = buildToolkit();
        ToolExecutionContext toolContext = buildToolExecutionContext(userId, agentUuid);

        SseStreamingHook sseHook = new SseStreamingHook();
        ContextCompressionHook compressionHook = new ContextCompressionHook(memory);

        ReActAgent reactAgent = buildReActAgent(key, agent, model, toolkit, memory, sseHook, compressionHook, toolContext);

        return new TeacherHarness(
                key, userId, agentUuid, scene, room.getId(), reactAgent, memory,
                chatMessageMapper, sseHook, compressionHook);
    }

    /**
     * 访客模式创建：不持久化、仅 PUBLISHED 场景、agent 必须已发布。
     */
    private TeacherHarness createGuestHarness(String key, String guestId, String agentUuid) {
        AgentDO agent = loadAndValidatePublishedAgent(agentUuid);

        DashScopeChatModel model = buildStreamingModel();
        DashScopeChatModel compactModel = buildCompactModel();

        // 访客态：roomId / userId 均为 null，AutoContextMemory 内存压缩照常，持久化自动跳过
        AutoContextMemory memory = new AutoContextMemory(
                compactModel, contextMemoryMapper, null, null, agentUuid);

        Toolkit toolkit = buildToolkit();
        ToolExecutionContext toolContext = buildGuestToolExecutionContext(guestId, agentUuid);

        SseStreamingHook sseHook = new SseStreamingHook();
        ContextCompressionHook compressionHook = new ContextCompressionHook(memory);

        ReActAgent reactAgent = buildReActAgent(key, agent, model, toolkit, memory, sseHook, compressionHook, toolContext);

        return new TeacherHarness(
                key, null, agentUuid, ChatSceneEnum.PUBLISHED, null, reactAgent, memory,
                chatMessageMapper, sseHook, compressionHook);
    }

    private ReActAgent buildReActAgent(String key,
                                       AgentDO agent,
                                       DashScopeChatModel model,
                                       Toolkit toolkit,
                                       AutoContextMemory memory,
                                       SseStreamingHook sseHook,
                                       ContextCompressionHook compressionHook,
                                       ToolExecutionContext toolContext) {
        ReActAgent.Builder builder = ReActAgent.builder()
                .name("Teacher_" + key)
                .sysPrompt(agent.getSystemPrompt())
                .model(model)
                .toolkit(toolkit)
                .memory(memory)
                .maxIters(maxIterations)
                .checkRunning(false)
                .hooks(List.of(sseHook, compressionHook))
                .toolExecutionContext(toolContext);

        String externalId = resolveKbExternalId(agent.getKnowledgeBaseId());
        Knowledge knowledge = buildKnowledge(externalId);
        if (knowledge != null) {
            builder.knowledge(knowledge).ragMode(RAGMode.AGENTIC);
            log.info("Agent已绑定知识库，启用Agentic RAG：agentUuid={}, knowledgeBaseId={}",
                    agent.getUuid(), agent.getKnowledgeBaseId());
        }
        return builder.build();
    }

    /**
     * 根据 ml_knowledge_base.id 查询百炼外部知识库ID。
     */
    private String resolveKbExternalId(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            return null;
        }
        KnowledgeBaseDO kbDO = knowledgeBaseMapper.selectById(knowledgeBaseId);
        return kbDO != null ? kbDO.getExternalId() : null;
    }

    /**
     * 根据知识库ID构建 BailianKnowledge，未配置百炼或 knowledgeBaseId 为空时返回 null。
     */
    private Knowledge buildKnowledge(String knowledgeBaseId) {
        if (knowledgeBaseId == null || bailianAccessKeyId.isEmpty() || bailianAccessKeySecret.isEmpty()) {
            return null;
        }
        BailianConfig config = BailianConfig.builder()
                .accessKeyId(bailianAccessKeyId)
                .accessKeySecret(bailianAccessKeySecret)
                .workspaceId(bailianWorkspaceId)
                .indexId(knowledgeBaseId)
                .enableReranking(true)
                .denseSimilarityTopK(20)
                .build();
        return BailianKnowledge.builder().config(config).build();
    }

    private AgentDO loadAndValidateAgent(String agentUuid) {
        LambdaQueryWrapper<AgentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentDO::getUuid, agentUuid)
               .last("LIMIT 1");
        AgentDO agent = agentMapper.selectOne(wrapper);
        if (agent == null) {
            throw new BizException(ErrorCode.PARAM_ILLEGAL.getCode(), "智能体不存在");
        }
        return agent;
    }

    private AgentDO loadAndValidatePublishedAgent(String agentUuid) {
        AgentDO agent = loadAndValidateAgent(agentUuid);
        if (agent.getStatus() != AgentStatusEnum.PUBLISHED) {
            throw new BizException(ErrorCode.PARAM_ILLEGAL.getCode(), "智能体未发布");
        }
        return agent;
    }

    private DashScopeChatModel buildStreamingModel() {
        return DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .stream(true)
                .build();
    }

    private DashScopeChatModel buildCompactModel() {
        return DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .stream(false)
                .build();
    }

    private void loadHistoryToMemory(Long userId, String agentUuid, AutoContextMemory memory) {
        List<ChatMessageDO> history = loadHistory(userId, agentUuid);
        for (ChatMessageDO h : history) {
            MsgRole role = h.getRole() == ChatRoleEnum.USER ? MsgRole.USER : MsgRole.ASSISTANT;
            Msg msg = Msg.builder()
                    .role(role)
                    .textContent(h.getContent())
                    .build();
            memory.add(msg);
        }
    }

    private List<ChatMessageDO> loadHistory(Long userId, String agentUuid) {
        LambdaQueryWrapper<ChatMessageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessageDO::getUserId, userId)
               .eq(ChatMessageDO::getAgentUuid, agentUuid)
               .orderByDesc(ChatMessageDO::getGmtCreated, ChatMessageDO::getId)
               .last("LIMIT " + MAX_HISTORY);
        List<ChatMessageDO> list = chatMessageMapper.selectList(wrapper);
        Collections.reverse(list);
        return list;
    }

    // TODO 后续注册工具时，依赖用户上下文的工具需检查 HarnessUserContext.isGuest()，访客模式直接返回提示
    private Toolkit buildToolkit() {
        Toolkit toolkit = new Toolkit();
        return toolkit;
    }

    private ToolExecutionContext buildToolExecutionContext(Long userId, String agentUuid) {
        return ToolExecutionContext.builder()
                .register(HarnessUserContext.ofUser(userId, agentUuid))
                .build();
    }

    private ToolExecutionContext buildGuestToolExecutionContext(String guestId, String agentUuid) {
        return ToolExecutionContext.builder()
                .register(HarnessUserContext.ofGuest(guestId, agentUuid))
                .build();
    }

    /**
     * 把当前内存中所有属于该访客的 harness 归并到登录账号下：
     * 把消息列表写入 ml_chat_message、压缩摘要写入 ml_context_memory，并销毁访客 harness。
     */
    public ClaimResult claimGuestHarnesses(String guestId, Long userId) {
        String prefix = GUEST_KEY_PREFIX + guestId + "_";
        String suffix = "_" + ChatSceneEnum.PUBLISHED.name();
        List<String> keys = harnessMap.keySet().stream()
                .filter(k -> k.startsWith(prefix) && k.endsWith(suffix))
                .toList();

        int totalMessages = 0;
        List<String> claimedAgents = new ArrayList<>();
        for (String key : keys) {
            TeacherHarness harness = harnessMap.remove(key);
            if (harness == null) {
                continue;
            }
            String agentUuid = key.substring(prefix.length(), key.length() - suffix.length());
            try {
                int written = claimSingleHarness(harness, agentUuid, userId);
                if (written > 0) {
                    totalMessages += written;
                    claimedAgents.add(agentUuid);
                }
            } catch (Exception e) {
                log.error("归并访客 harness 失败：key={}, error={}", key, e.getMessage(), e);
            }
        }
        log.info("访客归并完成：guestId={}, userId={}, totalMessages={}, agents={}",
                guestId, userId, totalMessages, claimedAgents);
        return new ClaimResult(totalMessages, claimedAgents);
    }

    private int claimSingleHarness(TeacherHarness harness, String agentUuid, Long userId) {
        Optional<TeacherHarness.ClaimSnapshot> opt = harness.tryClaim();
        if (opt.isEmpty()) {
            log.warn("归并跳过(锁占用或被中断)：agentUuid={}", agentUuid);
            return 0;
        }
        TeacherHarness.ClaimSnapshot snapshot = opt.get();
        ChatRoomDO room = chatRoomService.getOrCreate(userId, agentUuid, ChatSceneEnum.PUBLISHED);

        int written = writeMessagesToRoom(snapshot.messages(), room.getId(), userId, agentUuid);
        if (snapshot.persistedMemory() != null && !snapshot.persistedMemory().isBlank()) {
            ContextMemoryDO memoryDO = new ContextMemoryDO();
            memoryDO.setRoomId(room.getId());
            memoryDO.setUserId(userId);
            memoryDO.setAgentId(agentUuid);
            memoryDO.setContent(snapshot.persistedMemory());
            memoryDO.setMessageCount(snapshot.messages().size());
            contextMemoryMapper.insert(memoryDO);
        }
        log.info("归并成功：agentUuid={}, written={}, hasSummary={}",
                agentUuid, written, snapshot.persistedMemory() != null);
        return written;
    }

    private int writeMessagesToRoom(List<Msg> messages, Long roomDbId, Long userId, String agentUuid) {
        int written = 0;
        LocalDateTime base = LocalDateTime.now();
        int idx = 0;
        for (Msg msg : messages) {
            ChatRoleEnum role = mapMsgRole(msg.getRole());
            if (role == null) {
                continue; // 跳过 sysPrompt / 对话记忆等 SYSTEM 消息
            }
            String content = msg.getTextContent();
            if (content == null || content.isBlank()) {
                continue;
            }
            ChatMessageDO entity = new ChatMessageDO();
            entity.setMessageId(UUID.randomUUID().toString());
            entity.setRoomId(roomDbId);
            entity.setUserId(userId);
            entity.setAgentUuid(agentUuid);
            entity.setRole(role);
            entity.setContent(content);
            entity.setScene(ChatSceneEnum.PUBLISHED);
            entity.setGmtCreated(base.plusNanos((long) idx * 1_000_000L));
            chatMessageMapper.insert(entity);
            idx++;
            written++;
        }
        return written;
    }

    private ChatRoleEnum mapMsgRole(MsgRole role) {
        if (role == MsgRole.USER) {
            return ChatRoleEnum.USER;
        }
        if (role == MsgRole.ASSISTANT) {
            return ChatRoleEnum.ASSISTANT;
        }
        return null;
    }

    public record ClaimResult(int messageCount, List<String> agentUuids) {}
}
