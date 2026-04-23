package com.mylife.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AgentSaveDTO {

    private String uuid;

    @NotBlank(message = "智能体名称不能为空")
    @Size(max = 64, message = "名称最多64个字符")
    private String name;

    @Size(max = 256, message = "描述最多256个字符")
    private String description;

    private Integer iconIndex;

    private String color;

    private String systemPrompt;

    private Long knowledgeBaseId;
}
