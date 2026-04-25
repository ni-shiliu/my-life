# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

my-life 是一个 AI 智能教学平台，核心功能是个性化 AI 老师，通过 ReAct Agent 模式与学生实时对话，提供自适应教学。

## 技术栈

- **后端**: Java 21, Spring Boot 3.3.6, MyBatis-Plus 3.5.9, MySQL, JWT (jjwt 0.12.6)
- **AI 框架**: AgentScope Java SDK 1.0.11（ReActAgent）+ DashScopeChatModel
- **知识库**: 阿里百炼 SDK（BailianKnowledge）用于 Agentic RAG
- **前端**: Vue 3.5, TypeScript, Pinia, Vue Router 4, Axios, Vite 6, Sass
- **实时通信**: SSE（Server-Sent Events），非 WebSocket

## 常用命令

**后端**
```bash
cd api && ./mvnw spring-boot:run    # 启动（dev profile）
./mvnw test                          # 运行测试（当前无测试文件）
```

**前端**
```bash
cd web && npm run dev                # 开发服务器 (localhost:5173)
npm run build                       # 构建（vue-tsc 类型检查 + vite build）
```

**数据库初始化**: `api/sql/init.sql` — 6张表：`ml_user`, `ml_agent`, `ml_chat_room`, `ml_chat_message`, `ml_context_memory`, `ml_knowledge_base`

## 核心架构

聊天链路的关键组件及职责：

```
学生 → ChatController(SSE) → ChatServiceImpl → HarnessRegistry → TeacherHarness → ReActAgent
                                                                                      ↓
                                                                        ContextCompressionHook
                                                                        AutoContextMemory
                                                                        SseStreamingHook
                                                                        BailianKnowledge (RAG)
                                                                        MySQL (持久化)
```

- **ChatRoomService** (`service/impl/ChatRoomServiceImpl`): 管理聊天室（`ml_chat_room`），每个 `(userId, agentUuid, scene)` 对应一个聊天室。PUBLISHED 场景 uuid 有值，EDIT 场景 uuid 为 null。消息通过 `room_id` 关联聊天室。
- **HarnessRegistry** (`service/harness/`): Spring Bean，按 `userId_agentUuid_scene` 管理 TeacherHarness 实例生命周期（`ConcurrentHashMap.computeIfAbsent` 创建/复用，remove 销毁）
- **TeacherHarness**: 非 Spring Bean，持有会话状态和 roomId，`ReentrantLock` 防并发，`lastActiveAt` 追踪空闲
- **ReActAgent**: AgentScope SDK 的 ReAct 模式，maxIterations 由 `llm.max-iterations` 配置（默认10）
- **AutoContextMemory**: 实现 SDK `Memory` 接口，非 Spring Bean，消息超 30 条时保留最近 10 条 + LLM 摘要
- **SseStreamingHook**: 实现 SDK `Hook` 接口，拦截 ReasoningChunk/PreActing/PostActing/ErrorEvent 推送 SSE
- **ContextCompressionHook**: 实现 SDK `Hook` 接口，PreReasoning 时做 micro_compact + reminder 注入，PostActing 时追踪计划推进

### SSE 事件流

前端通过 `POST /v1/chat/send` 获取 `SseEmitter`，接收以下事件类型：
- `STREAM_CHUNK` — LLM 流式文本片段
- `TOOL_CALL` — Agent 调用工具
- `ERROR` — 错误信息
- `STREAM_END` — 对话结束（携带完整回复文本）

### 三层数据加载

### 聊天室模型

每个 `(userId, agentUuid, scene)` 组合对应一个 `ml_chat_room` 记录。消息通过 `room_id` 关联聊天室。
- EDIT 场景：聊天室 `uuid` 为 null（临时预览），清空时按 roomId 删除
- PUBLISHED 场景：聊天室 `uuid` 有值（正式对话），清空时按 roomId 删除
- 每条消息必须有 `message_id`（UUID），通过 `room_id` 关联聊天室

### 三层数据加载

新建 Harness 时按三层加载并构建个性化上下文：
1. **Layer 1**: 最近 20 条聊天记录（MySQL → AutoContextMemory）
2. **Layer 2**: 上下文记忆恢复（`ml_context_memory` 最新一条 → 摘要注入）
3. **Layer 3**: Agent 配置（`ml_agent.system_prompt` → ReActAgent.sysPrompt）

### ReAct 循环中的压缩机制

每轮 PreReasoning 触发 `ContextCompressionHook`：
1. **micro_compact**: 替换旧工具结果为 `[toolName结果已压缩]`，保留最近 2 轮
2. **auto_compact**: AutoContextMemory 检查消息数 > 30 → 保留 10 条 + LLM 摘要
3. **reminder**: 连续 3 轮未调用 advance/finish 工具 → 注入 `<reminder>` 催促

### LLM 可用工具

Agent 以工具形式对外暴露能力，LLM 自主选择调用：
- `calculate` — 数学计算（`service/tool/CalculateTool`）
- `updateStudentProfile` — 更新学生画像（`service/tool/UpdateStudentProfileTool`）
- `advanceTeachingPlan` — 推进教学计划（`service/tool/AdvanceTeachingPlanTool`）
- 知识库 RAG 由 AgentScope SDK 内置 `retrieve_knowledge` 处理（Agentic RAG mode, top-3, scoreThreshold=0.4）

### 前端代理配置

`vite.config.ts` 将 `/api` 前缀代理到后端 `http://localhost:8080`，并 rewrite 移除 `/api` 前缀。前端 API 请求统一走 `/api/v1/...`。

## 项目结构

```
my-life/
├── api/          # Spring Boot 后端 (Java 21, Maven)
│   ├── sql/init.sql
│   └── src/main/java/com/mylife/
│       ├── common/       # BaseResult, BizException, ErrorCode, GlobalExceptionHandler
│       ├── config/       # SecurityConfig, MyBatisMetaObjectHandler, ApiLogAspect
│       ├── controller/   # UserController, AgentController, ChatController, KnowledgeBaseController
│       ├── dto/          # 请求/响应 DTO
│       ├── entity/       # {Domain}DO 实体类
│       ├── enums/        # ChatRoleEnum, ChatSceneEnum, AgentStatusEnum
│       ├── mapper/       # MyBatis-Plus Mapper
│       ├── security/     # JwtAuthenticationFilter, LoginUser, SecurityUtils
│       ├── service/      # I{Domain}Service 接口
│       │   ├── harness/  # 核心会话管理（HarnessRegistry, TeacherHarness, Hooks, AutoContextMemory）
│       │   └── tool/     # Agent 工具（CalculateTool, UpdateStudentProfileTool, AdvanceTeachingPlanTool）
│       │   └── impl/     # Service 实现（ChatRoomServiceImpl 管理聊天室）
│       └── util/
├── web/          # Vue 3 前端
│   └── src/
│       ├── api/          # Axios 请求封装（request.ts, user.ts, agent.ts, chat.ts）
│       ├── composables/  # useAuth, useChat, useGuestAuth, usePasswordRule
│       ├── router/       # Vue Router 路由守卫
│       ├── stores/       # Pinia stores（user, agent）
│       ├── types/        # TypeScript 类型定义（agent, chat, user, knowledgeBase）
│       └── views/        # LoginView, RegisterView, HomeView, ChatView, AgentEditView
└── doc/          # 架构文档 (PlantUML)
```

## 命名规范

| 类型 | 规则 | 示例 |
|------|------|------|
| 实体类 | {Domain}DO | AgentDO |
| DTO请求 | {Domain}SaveDTO/UpdateDTO/QueryDTO/PageQueryDTO | TagSaveDTO |
| DTO响应 | {Domain}DTO/{Domain}DetailDTO | TagDetailDTO |
| 服务接口 | I{Domain}Service | IAgentService |
| 服务实现 | {Domain}ServiceImpl | AgentServiceImpl |
| Mapper | {Domain}Mapper | AgentMapper |
| Controller | {Domain}Controller | AgentController |
| 表名 | ml_{snake_case} | ml_agent, ml_chat_message |
| Enum值 | 大写英文 | ACTIVE, DRAFT |

## 硬性编码规则

### NEVER — 循环内查询
禁止在循环内查询数据库或调用远程接口。MUST 批量查询后用 Map 匹配。

### NEVER — JOIN语句
禁止SQL中使用JOIN。通过多次单表查询实现数据关联。

### NEVER — try-catch
全局异常处理器已配置。业务异常使用 `throw new BizException(ErrorCode.PARAM_ILLEGAL.getCode(), "msg")`。

### NEVER — @Autowired/@Resource
MUST 使用 `@RequiredArgsConstructor` + `private final` 字段。

### MUST — 函数≤30行
超过30行必须按职责拆分（参数校验、数据查询、业务计算、持久化、后续处理）。

### MUST — 枚举类型
Entity/DO 的状态/类型字段必须使用枚举，不用 String。枚举定义在 enums 包。

### MUST — 单表查询用 LambdaQueryWrapper
```java
wrapper.eq(AgentDO::getIsDeleted, "N")  // 方法引用不加括号
       .eq(AgentDO::getStatus, status);
```

### MUST — 日志使用 JSONObject.toJSONString()
禁止 `log.info("参数：{}", obj)` 直接打印对象。敏感信息（密码、token）必须脱敏。

### 代码风格
- Java 8 Stream API 优化集合操作
- 嵌套最多3层
- 常量代替魔法值
- Map 提升查找性能（O(1)）

## API 设计规范

### 路径规则
- Service层：`/v1/{module}/{action}` — 如 `/v1/chat/send`
- BFF层：`/v1/{platform}/{module}/{action}` — 如 `/v1/operator/tag/save`

### 标准 action
| action | 方法 | 说明 |
|--------|------|------|
| save | POST | 新增/修改 |
| get/{id} | GET | 获取详情 |
| list | POST | 列表（无分页） |
| queryPage | POST | 列表（分页） |
| delete | DELETE | 删除 |

### 分页查询模式

**类型链路**：`Spring Pageable → MyBatis-Plus Page → IPage → Spring PageImpl → BaseResult<Page>（BFF层）`

- 页码：`pageable.getPageNumber()` 直接用，NEVER +1
- 排序：NEVER 在 LambdaQueryWrapper 中硬编码 orderBy，用 `applySortToPage` 统一处理
- Lambda语法：`DmTag::getTagName` ✓ / `DmTag::getTagName()` ✗
- 必需 import：`com.baomidou.mybatisplus.core.metadata.{IPage, OrderItem}`, `com.baomidou.mybatisplus.extension.plugins.pagination.Page`, `org.springframework.data.domain.{Pageable, Sort, PageImpl}`

### BFF层职责
- 调用 Facade 获取数据，用 `BaseResult.success(data)` 包装响应
- Service层和Facade层 NEVER 包装 BaseResult

## 数据库规范

- 命名：snake_case，字符集 utf8mb4，引擎 InnoDB
- 必需字段：`id`(BIGINT AUTO_INCREMENT), `is_deleted`(CHAR(1) DEFAULT 'N'), `gmt_created`(DATETIME), `creator`(VARCHAR(64)), `gmt_modified`(DATETIME), `modifier`(VARCHAR(64))
- 索引命名：唯一 `uniq_xxx`，普通 `idx_xxx`

## 参数校验

DTO 中用 `@NotBlank`/`@Size` 等注解，Controller 用 `@Valid @RequestBody`。

## Git 规范

- 分支：`feature/{name}`, `bugfix/{name}`
- 提交：`feat:`, `fix:`, `docs:`, `refactor:`, `test:`

## 验证

- web: 前端项目，修改后需要验证（`npm run build` 检查类型，`npm run dev` 运行）
- api: 后端项目不需要编译来验证

## 架构待办

以下内容属于远期规划，当前代码尚未实现：

| 优先级 | 内容 |
|--------|------|
| P0 | PlanNotebook↔MySQL同步 |
| P1 | 手动compact工具、子任务并发约束、技能按需加载、工具参数校验 |
| P2 | 子代理隔离、DAG依赖、后台执行 |
