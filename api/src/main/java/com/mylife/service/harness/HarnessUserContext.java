package com.mylife.service.harness;

/**
 * 工具执行上下文，携带当前会话的用户和智能体信息。
 * 通过 ToolExecutionContext 注册，工具中自动注入。
 * 登录态：userId 有值、guestId 为 null。
 * 访客态：userId 为 null、guestId 有值。
 */
public record HarnessUserContext(Long userId, String agentUuid, String guestId) {

    public static HarnessUserContext ofUser(Long userId, String agentUuid) {
        return new HarnessUserContext(userId, agentUuid, null);
    }

    public static HarnessUserContext ofGuest(String guestId, String agentUuid) {
        return new HarnessUserContext(null, agentUuid, guestId);
    }

    public boolean isGuest() {
        return guestId != null && !guestId.isBlank();
    }
}
