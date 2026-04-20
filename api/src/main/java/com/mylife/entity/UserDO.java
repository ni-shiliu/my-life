package com.mylife.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mylife.enums.UserStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ml_user")
public class UserDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;

    private String password;

    private String nickName;

    private String avatar;

    private UserStatusEnum status;

    private Integer loginFailCount;

    private LocalDateTime lastLoginAt;

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
