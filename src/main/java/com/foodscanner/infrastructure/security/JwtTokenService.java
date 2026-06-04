package com.foodscanner.infrastructure.security;

import com.foodscanner.application.port.TokenService;
import com.foodscanner.domain.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/**
 * Слой: infrastructure
 * Реализация TokenService на JWT (HS256, jjwt). Access-токен по умолчанию 24ч.
 */
public class JwtTokenService implements TokenService {

    private final SecretKey key;
    private final Duration  accessTtl;

    public JwtTokenService(String secret, Duration accessTtl) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = accessTtl;
    }

    @Override
    public String issueAccessToken(UUID contributorId, String username) {
        Date now = new Date();
        return Jwts.builder()
            .subject(contributorId.toString())
            .claim("username", username)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + accessTtl.toMillis()))
            .signWith(key)
            .compact();
    }

    @Override
    public AccessClaims verifyAccessToken(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
            return new AccessClaims(UUID.fromString(c.getSubject()), c.get("username", String.class));
        } catch (Exception e) {
            throw new InvalidTokenException("Invalid access token");
        }
    }
}
