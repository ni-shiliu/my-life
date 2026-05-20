package com.mylife.security;

import com.mylife.common.BizException;
import com.mylife.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class SecurityUtils {

    private static final String USER_KEY_PREFIX = "u:";

    private static final String GUEST_KEY_PREFIX = "g:";

    private SecurityUtils() {
    }

    public static Optional<LoginUser> getOptionalUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUser) {
            return Optional.of((LoginUser) principal);
        }
        return Optional.empty();
    }

    public static Long getUserId() {
        return getOptionalUser()
                .map(LoginUser::getUserId)
                .orElseThrow(() -> new BizException(ErrorCode.TOKEN_INVALID.getCode(), "获取用户ID失败"));
    }

    public static Optional<Long> getOptionalUserId() {
        return getOptionalUser().map(LoginUser::getUserId);
    }

    public static Optional<String> getOptionalGuestId() {
        return getOptionalUser().map(LoginUser::getGuestId);
    }

    public static boolean isGuest() {
        return getOptionalUser().map(LoginUser::isGuest).orElse(false);
    }

    public static String getPrincipalKey() {
        LoginUser loginUser = getOptionalUser()
                .orElseThrow(() -> new BizException(ErrorCode.TOKEN_INVALID.getCode(), "未登录"));
        if (loginUser.isGuest()) {
            return GUEST_KEY_PREFIX + loginUser.getGuestId();
        }
        if (loginUser.getUserId() == null) {
            throw new BizException(ErrorCode.TOKEN_INVALID.getCode(), "获取用户ID失败");
        }
        return USER_KEY_PREFIX + loginUser.getUserId();
    }
}
