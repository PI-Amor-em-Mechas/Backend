package br.com.amorEmMechas_Formulario.api.para.formulario.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Provedor de tokens JWT conforme requisitos de segurança HL7.
 * Implementa autenticação stateless com tokens assinados (JWS).
 */
@Component
public class JwtTokenProvider {

    @Value("${hl7.security.jwt.secret}")
    private String jwtSecret;

    @Value("${hl7.security.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${hl7.security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Value("${hl7.security.application-token-expiration-ms}")
    private long applicationTokenExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String role = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .findFirst()
                .orElse(null);
        return generateTokenFromUsername(userDetails.getUsername(), role);
    }

    public String generateTokenFromUsername(String username) {
        return generateTokenFromUsername(username, null);
    }

    public String generateTokenFromUsername(String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        var builder = Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("type", "access");
        if (role != null && !role.isBlank()) {
            builder.claim("role", role);
        }
        return builder.signWith(getSigningKey()).compact();
    }

    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("type", "refresh")
                .signWith(getSigningKey())
                .compact();
    }

    public String generateDevToken(String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        claims.put("dev", true);
        claims.put("role", role);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateApplicationToken(Integer patientId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + applicationTokenExpirationMs);

        return Jwts.builder()
                .subject("public-application")
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("type", "application")
                .claim("purpose", "public_application")
                .claim("scope", "upload_medical_report")
                .claim("patientId", patientId)
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateApplicationToken(String token, Integer patientId) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Number tokenPatientId = claims.get("patientId", Number.class);
            return "application".equals(claims.get("type", String.class))
                    && "public_application".equals(claims.get("purpose", String.class))
                    && "upload_medical_report".equals(claims.get("scope", String.class))
                    && tokenPatientId != null
                    && tokenPatientId.intValue() == patientId;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getTokenType(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("type", String.class);
    }

    public <T> T getClaim(String token, String name, Class<T> type) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get(name, type);
    }
}
