package com.mylife.controller;

import com.mylife.common.BaseResult;
import com.mylife.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * 访客身份接入。未登录用户通过 /v1/chat/guest/token 获取短期 JWT 直接接入分享链接对话。
 */
@Slf4j
@RestController
@RequestMapping("/v1/chat/guest")
@RequiredArgsConstructor
public class GuestController {

    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/token")
    public BaseResult<Map<String, Object>> token() {
        String guestId = UUID.randomUUID().toString();
        String token = jwtTokenProvider.generateGuestToken(guestId);
        log.info("生成访客令牌：guestId={}", guestId);
        return BaseResult.success(Map.of("token", token, "sessionId", guestId));
    }
}
