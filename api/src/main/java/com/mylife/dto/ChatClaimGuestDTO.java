package com.mylife.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatClaimGuestDTO {

    @NotBlank(message = "guestToken不能为空")
    private String guestToken;
}
