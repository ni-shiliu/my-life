package com.mylife.service.harness;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mylife.common.BizException;
import com.mylife.common.ErrorCode;
import com.mylife.entity.AgentDO;
import com.mylife.entity.ChatRoomDO;
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

import java.util.Collections;
import java.util.List;
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
               .orderByDesc(ChatMessageDO::getGmtCreated)
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
}
