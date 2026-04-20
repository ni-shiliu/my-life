package com.mylife.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS("0", "success"),
    USER_PHONE_EXISTS("USER_PHONE_EXISTS", "该手机号已注册"),
    USER_PASSWORD_ERROR("USER_PASSWORD_ERROR", "手机号或密码错误"),
    USER_DISABLED("USER_DISABLED", "账户已被禁用，请联系客服"),
    USER_LOCKED("USER_LOCKED", "账户已锁定"),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "Token已过期，请重新登录"),
    TOKEN_INVALID("TOKEN_INVALID", "无效的Token"),
    RATE_LIMITED("RATE_LIMITED", "请求过于频繁，请稍后再试"),
    PARAM_ILLEGAL("PARAM_ILLEGAL", "参数校验失败"),
    SYSTEM_ERROR("SYSTEM_ERROR", "系统异常");

    private final String code;
    private final String message;
}
