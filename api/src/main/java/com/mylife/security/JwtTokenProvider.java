package com.mylife.security;

import com.mylife.common.BizException;
import com.mylife.common.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final String TYPE_ACCESS = "access";

    private static final String TYPE_GUEST = "guest";

    private static final String CLAIM_TYPE = "type";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-seconds}")
    private long expireSeconds;

    @Value("${guest.token-expire-seconds}")
    private long guestExpireSeconds;

    public String generateToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireSeconds * 1000);
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateGuestToken(String guestId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + guestExpireSeconds * 1000);
        return Jwts.builder()
                .subject(guestId)
                .claim(CLAIM_TYPE, TYPE_GUEST)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public JwtClaims validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return buildJwtClaims(claims);
        } catch (ExpiredJwtException e) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED.getCode(), ErrorCode.TOKEN_EXPIRED.getMessage());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.TOKEN_INVALID.getCode(), ErrorCode.TOKEN_INVALID.getMessage());
        }
    }

    /**
     * 校验签名但容忍过期。用于访客对话归并：登录后允许用过期 guest token 取出 guestId，
     * 不验签则会被恶意构造冒领他人会话。
     */
    public JwtClaims validateAllowExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return buildJwtClaims(claims);
        } catch (ExpiredJwtException e) {
            return buildJwtClaims(e.getClaims());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.TOKEN_INVALID.getCode(), ErrorCode.TOKEN_INVALID.getMessage());
        }
    }

    private JwtClaims buildJwtClaims(Claims claims) {
        String type = claims.get(CLAIM_TYPE, String.class);
        if (TYPE_ACCESS.equals(type)) {
            return JwtClaims.builder()
                    .type(TYPE_ACCESS)
                    .userId(Long.parseLong(claims.getSubject()))
                    .issuedAt(claims.getIssuedAt())
                    .expiration(claims.getExpiration())
                    .build();
        }
        if (TYPE_GUEST.equals(type)) {
            String guestId = claims.getSubject();
            if (!StringUtils.hasText(guestId)) {
                throw new BizException(ErrorCode.TOKEN_INVALID.getCode(), ErrorCode.TOKEN_INVALID.getMessage());
            }
            return JwtClaims.builder()
                    .type(TYPE_GUEST)
                    .guestId(guestId)
                    .issuedAt(claims.getIssuedAt())
                    .expiration(claims.getExpiration())
                    .build();
        }
        throw new BizException(ErrorCode.TOKEN_INVALID.getCode(), ErrorCode.TOKEN_INVALID.getMessage());
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
