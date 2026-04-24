package com.mylife.dto;

import lombok.Data;

@Data
public class KnowledgeBaseDTO {

    private Long id;

    private String uuid;

    private String name;

    private String source;

    private String externalId;

    private String gmtModified;
}
