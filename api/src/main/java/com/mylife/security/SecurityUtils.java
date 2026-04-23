package com.mylife.security;

import com.mylife.common.BizException;
import com.mylife.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class SecurityUtils {

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
}
