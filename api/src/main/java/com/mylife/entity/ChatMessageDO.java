package com.mylife.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mylife.enums.ChatRoleEnum;
import com.mylife.enums.ChatSceneEnum;
import com.mylife.enums.YesNoEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ml_chat_message")
public class ChatMessageDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String messageId;

    private Long roomId;

    private Long userId;

    private String agentUuid;

    private ChatRoleEnum role;

    private String content;

    @TableField("tool_name")
    private String toolName;

    private ChatSceneEnum scene;

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
