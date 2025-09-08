package com.polarisoffice.secuone.util;

import java.security.Key;
import java.util.Map;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import io.jsonwebtoken.security.Keys;

public class JwtUtil {
	  // ★ 실제 운영은 환경변수/설정값으로! (32바이트 이상)
	  private static final String SECRET = "change-this-to-a-very-long-32+byte-secret-key";
	  private static final Key KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

	  public static String createToken(String sub, String role, long ttlMillis){
	    long now = System.currentTimeMillis();
	    return Jwts.builder()
	        .setSubject(sub)
	        .addClaims(Map.of("role", role))
	        .setIssuedAt(new Date(now))
	        .setExpiration(new Date(now + ttlMillis))
	        .signWith(KEY, SignatureAlgorithm.HS256)
	        .compact();
	  }

	  public static Jws<Claims> parse(String token){
	    return Jwts.parserBuilder().setSigningKey(KEY).build().parseClaimsJws(token);
	  }
}
