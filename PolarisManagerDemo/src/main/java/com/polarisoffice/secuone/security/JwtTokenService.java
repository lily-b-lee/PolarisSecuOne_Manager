package com.polarisoffice.secuone.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtTokenService {

    private final SecretKey key;
    private final long validityMs;

    public JwtTokenService(String secret, long validityMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validityMs = validityMs;
    }

    public String create(String subject) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + validityMs);

        return Jwts.builder()
                .subject(subject)                // 0.12 스타일
                .issuedAt(now)
                .expiration(exp)
                .signWith(key, Jwts.SIG.HS256)   // (SecretKey, MacAlgorithm)
                .compact();
    }

    /** 0.12: parser().verifyWith(key).build().parseSignedClaims(token).getPayload() */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 기존 호출부(JwtAuthFilter)가 기대하는 시그니처 맞추기용 */
    public Claims decode(String token) {
        return parse(token);
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
