package com.mylife.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    /**
     * 登录用户 id（访客态为 null）。
     */
    private Long userId;

    /**
     * 访客 id（登录态为 null）。
     */
    private String guestId;

    public static LoginUser ofUser(Long userId) {
        return new LoginUser(userId, null);
    }

    public static LoginUser ofGuest(String guestId) {
        return new LoginUser(null, guestId);
    }

    public boolean isGuest() {
        return StringUtils.hasText(guestId);
    }
}
