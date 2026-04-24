package com.mylife.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeBaseSaveDTO {

    private String uuid;

    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 64, message = "名称最多64个字符")
    private String name;

    @NotBlank(message = "百炼知识库ID不能为空")
    @Size(max = 64, message = "百炼知识库ID最多64个字符")
    private String externalId;
}
