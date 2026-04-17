# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

my-life 是一个 AI 智能教学平台（neil-edu），核心功能是个性化 AI 老师，通过 ReAct Agent 模式与学生实时对话，提供自适应教学。

## 技术栈

- **语言**: Java 21 + springboot 4.X+
- **AI 框架**: AgentScope SDK（ReActAgent）
- **数据库**: MySQL（持久化）、Qdrant（向量知识库）
- **Embedding**: text-embedding-v3, 1024维
- **实时通信**: WebSocket
- **IDE**: IntelliJ IDEA（JPA Buddy、SonarLint、Checkstyle）

## 核心架构

聊天链路的关键组件及职责：

```
学生 → WebSocket Controller → ChatService → HarnessRegistry → TeacherHarness → ReActAgent
                                                                              ↓
                                                                    PlanNotebook (SDK内置)
                                                                    向量库 (Qdrant)
                                                                    MySQL
```

- **HarnessRegistry**: 按 `userId_agentId` 管理 TeacherHarness 实例的生命周期（创建/复用/回收）
- **TeacherHarness**: 持有会话状态，处理并发锁（lock + lastActiveAt），封装 Agent 调用
- **ReActAgent**: 基于 AgentScope SDK 的 ReAct 模式 Agent，上限 20 轮迭代
- **AutoContextMemory**: 自动上下文压缩——消息超 30 条时保留最近 10 条 + LLM 摘要
- **PlanNotebook**: SDK 内置的计划管理（create_plan / finish_subtask / finish_plan）

### 三层数据加载

新建 Harness 时按三层加载并构建个性化 sysPrompt：
1. **Layer 1**: 最近 20 条聊天记录（MySQL）
2. **Layer 2**: StudentProfile 学生画像，含薄弱点（MySQL）
3. **Layer 3**: TeachingPlan 教学计划与进度（MySQL）

### LLM 可用工具

Agent 以工具形式对外暴露能力，LLM 自主选择调用：
- `calculate` — 数学计算
- `retrieve_knowledge` — 向量库检索教材（top-3, scoreThreshold=0.4, agentId 隔离）
- `updateStudentProfile` — 更新学生画像（写 MySQL Layer 2）
- `advanceTeachingPlan` — 推进教学计划（写 MySQL Layer 3）
- `create_plan` / `finish_subtask` / `finish_plan` — 计划管理

### ReAct 循环（每轮 6 步，含补充环节）

1. 上下文压缩：
   - **1a. micro_compact**：静默替换旧工具结果为占位符（如 `[retrieve_knowledge结果已压缩]`），最有效的 token 节省手段
   - **1b. auto_compact**：AutoContextMemory 检查消息数，超 30 条 → 保留 10 条 + LLM 摘要
   - **1c. 催促提醒**：连续 N 轮未调用 advanceTeachingPlan/finish_subtask → 注入 `<reminder>` 催促
2. 调用 LLM（流式）
3. 解析响应（TextBlock → 退出，ToolUseBlock → 继续）
4. 执行工具（含参数校验，如 retrieve_knowledge 的 query 长度≤200、注入防护）
5. 工具结果追加到对话历史
6. 判断：有 ToolUseBlock → 回 Step 1，无 → 退出返回；maxIterations/maxTokens → 强制退出

## 架构演进待办（对标通用 Agent Harness 12 层规范）

基于 `doc/通用Agent-Harness总结.md` 的对比分析，neil-edu 相对通用 Harness 的缺失项：

### P0 — 直接影响教学质量

- **micro_compact（s06）**：每轮静默替换旧工具结果为占位符，知识检索结果长时不做替换会快速占满上下文
- **规划催促提醒（s03）**：Agent 连续多轮未推进教学计划时注入 `<reminder>`，防止长对话迷失
- **PlanNotebook ↔ MySQL 同步（s07）**：finish_subtask/finish_plan 时同步写 MySQL，防 Harness 回收后进度丢失

### P1 — 提升系统健壮性

- **手动 compact 工具（s06）**：让 Agent 主动压缩上下文
- **子任务并发约束（s03）**：同一时刻仅一个 in_progress 子任务
- **技能按需加载（s05）**：教学策略等重内容不在 sysPrompt 常驻，改为 load_skill 按需注入
- **工具参数安全校验（s02）**：至少对 retrieve_knowledge 的 query 做注入防护

### P2 — 架构扩展性

- **子代理隔离（s04）**：为复杂工具（如多步练习题生成）设计独立消息历史
- **DAG 依赖（s07）**：教学步骤间先后依赖关系建模
- **后台执行（s08）**：耗时操作异步化

### 暂不需要

- s09-s11 多代理协作（单老师场景）、s12 工作树隔离（无文件操作）、s02 路径沙箱（无文件系统工具）

## 项目状态

项目处于早期阶段，目前仅有架构设计文档（PlantUML 序列图），尚无实际代码实现。Java 构建工具（Maven/Gradle）待确定。

## 命名规范

| 类型 | 规则 | 示例 |
|------|------|------|
| 实体类 | {Domain}DO | OmsOrderDO |
| DTO请求 | {Domain}SaveDTO/UpdateDTO/QueryDTO/PageQueryDTO | TagSaveDTO |
| DTO响应 | {Domain}DTO/{Domain}DetailDTO | TagDetailDTO |
| 服务接口 | I{Domain}Service | IOrderService |
| 服务实现 | {Domain}ServiceImpl | OrderServiceImpl |
| Facade | {Domain}Facade | OrderFacade |
| Mapper | {Domain}Mapper | OrderMapper |
| Controller | {Domain}Controller | OrderController |
| 表名 | {prefix}_{snake_case} | oms_order, dm_tag |
| Enum值 | 大写英文 | ACTIVE, PAID_ORDER_COUNT |

## 硬性编码规则

### NEVER — 循环内查询
禁止在循环内查询数据库或调用远程接口。MUST 批量查询后用 Map 匹配：
```java
List<GoodsDTO> list = goodsFacade.batchGetGoods(ids).getResult();
Map<Long, GoodsDTO> map = list.stream().collect(Collectors.toMap(GoodsDTO::getId, Function.identity()));
```

### NEVER — JOIN语句
禁止SQL中使用JOIN。通过多次单表查询 + Facade调用实现数据关联。

### NEVER — try-catch
全局异常处理器已配置。业务异常使用 `throw new BizException(ErrorCode.PARAM_ILLEGAL.getCode(), "msg")`。

### NEVER — @Autowired/@Resource
MUST 使用 `@RequiredArgsConstructor` + `private final` 字段：
```java
@RequiredArgsConstructor
public class TagController {
    private final ITagService tagService;
}
```

### MUST — 函数≤30行
超过30行必须按职责拆分（参数校验、数据查询、业务计算、持久化、后续处理）。

### MUST — 枚举类型
Entity/DO 的状态/类型字段必须使用枚举，不用 String。枚举定义在 share 的 enums 包。

### MUST — 单表查询用 LambdaQueryWrapper
```java
wrapper.eq(DmTag::getIsDeleted, YesNoEnum.NO.getValue())  // 注意：方法引用不加括号
       .eq(DmTag::getStatus, status);
```

### MUST — 日志使用 JSONObject.toJSONString()
禁止 `log.info("参数：{}", obj)` 直接打印对象。MUST：
```java
log.info("请求参数：{}", JSONObject.toJSONString(dto));
```
敏感信息（密码、token）必须脱敏。

### 代码风格
- 使用 Java 8 Stream API 优化集合操作
- 嵌套最多3层
- 常量代替魔法值
- 使用 Map 提升查找性能（O(1)）

## API 设计规范

### 路径规则
- Service层：`/v1/{module}/{action}` — 如 `/v1/tag/save`
- BFF层：`/v1/{platform}/{module}/{action}` — 如 `/v1/operator/tag/save`

### 标准 action
| action | 方法 | 说明 |
|--------|------|------|
| save | POST | 新增/修改 |
| get/{id} | GET | 获取详情 |
| list | POST | 列表（无分页） |
| queryPage | POST | 列表（分页） |
| delete | DELETE | 删除 

### 分页查询完整实现模式

**类型链路**：`Spring Pageable → MyBatis-Plus Page → IPage → Spring PageImpl → BaseResult<Page>（BFF层）`

**Service层**（核心实现）：
```java
@Override
public IPage<TagDTO> queryTagPage(TagPageQueryDTO pageQueryDTO, Pageable pageable) {
    LambdaQueryWrapper<DmTagDO> wrapper = buildQueryWrapper(pageQueryDTO);
    Page<DmTagDO> page = new Page<>(pageable.getPageNumber(), pageable.getPageSize()); // NEVER +1
    applySortToPage(page, pageable.getSort());
    IPage<DmTagDO> tagPage = page(page, wrapper);
    IPage<TagDTO> result = new Page<>();
    result.setRecords(tagPage.getRecords().stream().map(this::convertToDTO).collect(Collectors.toList()));
    result.setTotal(tagPage.getTotal());
    result.setCurrent(tagPage.getCurrent());
    result.setSize(tagPage.getSize());
    return result;
}

private void applySortToPage(Page<?> page, Sort sort) {
    if (sort.isUnsorted()) {
        page.setOrders(OrderItem.descs("id"));
    } else {
        List<OrderItem> items = new ArrayList<>();
        sort.forEach(o -> items.add(o.isAscending() ? OrderItem.asc(o.getProperty()) : OrderItem.desc(o.getProperty())));
        page.setOrders(items);
    }
}
// buildQueryWrapper 中 NEVER 设置 orderBy，排序由 applySortToPage 处理
```

**Controller层**（IPage → Spring PageImpl 转换）：
```java
@Override
@PostMapping("/queryPage")
public Page<TagDTO> queryPage(@RequestBody TagPageQueryDTO pageQueryDTO,
                               @SpringQueryMap Pageable pageable) {
    IPage<TagDTO> tagPage = tagService.queryTagPage(pageQueryDTO, pageable);
    return new PageImpl<>(tagPage.getRecords(), pageable, tagPage.getTotal());
}
```

**BFF层**（包装 BaseResult）：
```java
@PostMapping("/queryPage")
public BaseResult<Page<TagDTO>> queryPage(@RequestBody TagPageQueryDTO pageQueryDTO,
                                           @SpringQueryMap Pageable pageable) {
    return BaseResult.success(tagFacade.queryPage(pageQueryDTO, pageable));
}
```

**关键约束**：
- 页码：`pageable.getPageNumber()` 直接用，NEVER +1
- 排序：NEVER 在 LambdaQueryWrapper 中硬编码 orderBy
- Lambda语法：`DmTagDO::getTagName` ✓ / `DmTagDO::getTagName()` ✗
- 必需 import：`com.baomidou.mybatisplus.core.metadata.{IPage, OrderItem}`, `c.b.m.extension.plugins.pagination.Page`, `org.springframework.data.domain.{Pageable, Sort, PageImpl}`

### BFF层职责
- 调用 Facade 获取数据
- 用 `BaseResult.success(data)` 包装响应
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
