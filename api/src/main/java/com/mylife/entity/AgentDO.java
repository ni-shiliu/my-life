package com.mylife.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mylife.enums.AgentStatusEnum;
import com.mylife.enums.YesNoEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ml_agent")
public class AgentDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String description;

    @TableField("icon_index")
    private Integer iconIndex;

    private String color;

    private String systemPrompt;

    private Long knowledgeBaseId;

    private AgentStatusEnum status;

    @TableLogic
    private String isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreated;

    @TableField(fill = FieldFill.INSERT)
    private String creator;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String modifier;
}
