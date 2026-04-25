package com.mylife.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ml_user_agent")
public class UserAgentDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String agentUuid;

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
