package com.mylife.service.harness;

/**
 * 工具执行上下文，携带当前会话的用户和智能体信息。
 * 通过 ToolExecutionContext 注册，工具中自动注入。
 */
public record HarnessUserContext(Long userId, String agentUuid) {
}
