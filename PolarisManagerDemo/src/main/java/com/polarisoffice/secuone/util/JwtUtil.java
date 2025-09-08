package com.polarisoffice.secuone.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

public final class JwtUtil {
    private JwtUtil() {}

    public static Claims parse(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)         // 0.12
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static String getSubject(String token, String secret) {
        return parse(token, secret).getSubject();
    }
}
