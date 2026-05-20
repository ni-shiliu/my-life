package com.mylife.dto;

import lombok.Data;

/**
 * 已发布智能体的公开详情（访客可用）。
 * 仅暴露展示所需的最小字段，不含 system_prompt / owner 等敏感信息。
 */
@Data
public class AgentPublicDTO {

    private String uuid;

    private String name;

    private String description;

    private String color;

    private Integer iconIndex;
}
