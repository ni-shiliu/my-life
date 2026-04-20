package com.mylife.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDTO {

    private Long userId;
    private String phone;
    private String nickName;
    private String accessToken;
    private LocalDateTime accessExpireAt;
}
