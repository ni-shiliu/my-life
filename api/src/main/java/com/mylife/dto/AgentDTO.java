package com.mylife.dto;

import lombok.Data;

@Data
public class AgentDTO {

    private String uuid;

    private String name;

    private String description;

    private Integer iconIndex;

    private String color;

    private String systemPrompt;

    private Long knowledgeBaseId;

    private String status;

    private String gmtModified;
}
