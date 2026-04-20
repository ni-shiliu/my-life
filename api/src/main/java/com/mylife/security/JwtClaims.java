package com.mylife.security;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class JwtClaims {

    private Long userId;
    private Date issuedAt;
    private Date expiration;
}
