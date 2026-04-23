# my-life

AI 智能教学平台 —— 个性化 AI 老师，通过 ReAct Agent 模式与学生实时对话，提供自适应教学。

## 功能特性

- **AI 智能体管理**：创建、编辑、发布自定义 AI 老师（系统提示词、图标、颜色）
- **实时对话**：基于 SSE 流式推送，LLM 逐字输出，工具调用过程可见
- **自适应教学**：AI 老师可调用计算器、更新学生画像、推进教学计划
- **知识库检索**：绑定阿里百炼知识库，启用 Agentic RAG 按需检索教材
- **上下文记忆**：自动压缩长对话（micro_compact + auto_compact），跨会话恢复记忆
- **用户认证**：手机号注册登录，JWT 无状态认证

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 21, Spring Boot 3.3.6, MyBatis-Plus, MySQL |
| AI | AgentScope SDK (ReAct), DashScopeChatModel, 阿里百炼 RAG |
| 前端 | Vue 3, TypeScript, Pinia, Vue Router, Vite, Sass |
| 通信 | SSE (Server-Sent Events) |

## 快速开始

### 环境要求

- Java 21+
- MySQL 8.0+
- Node.js 18+

### 1. 初始化数据库

```bash
mysql -u root -p < api/sql/init.sql
```

### 2. 启动后端

```bash
cd api
./mvnw spring-boot:run
```

后端启动在 http://localhost:8080

数据库连接、LLM API Key 等配置见 `api/src/main/resources/application.yml`

### 3. 启动前端

```bash
cd web
npm install
npm run dev
```

前端启动在 http://localhost:5173，`/api` 请求自动代理到后端

## 项目结构

```
my-life/
├── api/     # Spring Boot 后端 (Java 21, Maven)
├── web/     # Vue 3 前端 (Vite, TypeScript, Pinia)
└── doc/     # 架构文档 (PlantUML)
```

详细的代码结构、命名规范和编码规则见 [CLAUDE.md](CLAUDE.md)。
