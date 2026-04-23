package com.mylife.dto;

import com.mylife.enums.ChatSceneEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatSendDTO {

    @NotBlank(message = "agentUuid不能为空")
    private String agentUuid;

    @NotBlank(message = "消息不能为空")
    @Size(max = 2000, message = "消息长度不能超过2000字")
    private String message;

    private ChatSceneEnum scene;
}
