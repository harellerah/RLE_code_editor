package com.example.backend;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {
    private static String secret;
    @Value("${jwt.expiration}") private long expiration;

    @Value("${jwt.secret}")
    public void setSecret(String secretValue) {
        JwtUtil.secret = secretValue; // inject into static
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .compact();
    }

    public static String extractUsername(String token) {
        token = token.startsWith("Bearer ")
                ? token.substring(7)
                : token;
        return Jwts.parserBuilder().setSigningKey(secret.getBytes()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }
}
