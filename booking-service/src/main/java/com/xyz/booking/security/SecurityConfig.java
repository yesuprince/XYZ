package com.xyz.booking.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    @Value("${spring.security.oauth2.resourceserver.jwt.secret-key}")
    private String jwtSecret;

    @Value("${spring.security.oauth2.resourceserver.jwt.algorithm:HS256}")
    private String jwtAlgorithm;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt.decoder(jwtDecoder()))
                )
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);

        String algorithmName = jwtAlgorithm.toUpperCase();

        return switch (algorithmName) {
            case "HS384" -> NimbusJwtDecoder
                    .withSecretKey(new SecretKeySpec(keyBytes, "HmacSHA384"))
                    .build();
            case "HS512" -> NimbusJwtDecoder
                    .withSecretKey(new SecretKeySpec(keyBytes, "HmacSHA512"))
                    .build();
            default -> NimbusJwtDecoder
                    .withSecretKey(new SecretKeySpec(keyBytes, "HmacSHA256"))
                    .build();
        };
    }
}