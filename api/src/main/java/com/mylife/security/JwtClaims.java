package com.mylife.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtClaims {

    /**
     * 令牌类型：access / guest。
     */
    private String type;

    /**
     * 登录用户 id（type=access 时有值）。
     */
    private Long userId;

    /**
     * 访客 id（type=guest 时有值）。
     */
    private String guestId;

    private Date issuedAt;

    private Date expiration;
}
