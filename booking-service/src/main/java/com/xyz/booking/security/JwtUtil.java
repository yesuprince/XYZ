package com.xyz.booking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Slf4j
public class JwtUtil {

    private static final String SECRET = "t3dxNrdVbFXBH1+ispTZur2aoyL/bVN1rx0pCfVFP44=";

    /**
     * Generate a JWT token with a "scope" claim (e.g. user / admin).
     */
    public static String generateToken(String username, String role) {
        Key signingKey = getSigningKey();
        Instant now = Instant.now();

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(username)
                .setIssuer("XYZ-car-rental")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .claim("scope", role)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validate token and return parsed JWS Claims. Throws JwtException on invalid token.
     */
    public static Jws<Claims> validateToken(String token) {
        Key signingKey = getSigningKey();
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);
    }

    /**
     * Generate a secure random secret key and return Base64 encoded string.
     */
    public static String generateSecretKey() {
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        return Encoders.BASE64.encode(key.getEncoded());
    }

    private static Key getSigningKey() {
        byte[] keyBytes = SECRET.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public static void main(String[] args) {
//        log.info("Generated secret (Base64): \n" + generateSecretKey());

        String userToken = generateToken("Customer", "user");
        log.info("User token:\n" + userToken);

        String adminToken = generateToken("XYZ", "admin");
        log.info("Admin token:\n" + adminToken);

        Jws<Claims> claims = validateToken(userToken);
        log.info("Decoded claims for user token: " + claims.getBody());
    }
}
