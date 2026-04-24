package com.mylife.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mylife.enums.KbSourceEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ml_knowledge_base")
public class KnowledgeBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String uuid;

    private Long userId;

    private String name;

    private KbSourceEnum source;

    private String externalId;

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
