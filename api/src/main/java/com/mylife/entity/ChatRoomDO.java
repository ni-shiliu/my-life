package com.mylife.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mylife.enums.ChatSceneEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ml_chat_room")
public class ChatRoomDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roomId;

    private Long userId;

    private String agentUuid;

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
