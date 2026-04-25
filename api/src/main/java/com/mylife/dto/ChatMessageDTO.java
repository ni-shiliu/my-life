package com.mylife.dto;

import com.mylife.enums.ChatRoleEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageDTO {

    private Long id;

    private String messageId;

    private Long roomId;

    private ChatRoleEnum role;

    private String content;

    private String toolName;

    private LocalDateTime gmtCreated;
}
