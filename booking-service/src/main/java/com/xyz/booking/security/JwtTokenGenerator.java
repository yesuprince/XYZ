package com.xyz.booking.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
public class JwtTokenGenerator {
    public static void main(String[] args) {
        String base64Secret = "t3dxNrdVbFXBH1+ispTZur2aoyL/bVN1rx0pCfVFP44=";

        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        Key key = Keys.hmacShaKeyFor(keyBytes);

        Instant now = Instant.now();

        String token = Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject("Customer")
                .setIssuer("XYZ-car-rental")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(3600)))
                .claim("scope", "user")
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        log.info("Token:\n{}", token);
    }
}
