package org.hkijena.jast.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.hkijena.jast.config.JwtConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Component
public class JwtUtil {

    private final JwtConfig jwtConfig;
    public static final String TOKEN_CLAIM_KEY_TYPE = "type";
    public static final String TOKEN_CLAIM_VALUE_TYPE_ACCESS = "access";
    public static final String TOKEN_CLAIM_VALUE_TYPE_REFRESH = "refresh";

    @Autowired
    public JwtUtil(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    public SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String extractUsername(String token, boolean verify) {
        return extractClaim(token, Claims::getSubject, verify);
    }

    public Date extractExpiration(String token, boolean verify) {
        return extractClaim(token, Claims::getExpiration, verify);
    }

    public boolean isAccessToken(String token, boolean verify) {
        return Objects.equals(extractClaim(token, claims -> claims.get(TOKEN_CLAIM_KEY_TYPE, String.class), verify), TOKEN_CLAIM_VALUE_TYPE_ACCESS);
    }

    public boolean isRefreshToken(String token, boolean verify) {
        return Objects.equals(extractClaim(token, claims -> claims.get(TOKEN_CLAIM_KEY_TYPE, String.class), verify), TOKEN_CLAIM_VALUE_TYPE_REFRESH);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver, boolean verify) {
        final Claims claims = extractAllClaims(token, verify);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token, boolean verify) {
        try {
            return Jwts.parser().verifyWith(getSecretKey()).build().parse(token).accept(Jws.CLAIMS).getPayload();
        }
        catch (ExpiredJwtException e) {
            if(!verify) {
                return e.getClaims();
            }
            else {
                throw e;
            }
        }
    }

    public Date getNewRefreshExpirationDate() {
        return new Date(System.currentTimeMillis() + jwtConfig.getJwtRefreshTokenExpirationInMinutes() * 60L * 1000);
    }

    public Date getNewAccessExpirationDate() {
        return new Date(System.currentTimeMillis() + jwtConfig.getJwtAccessTokenExpirationInMinutes() * 60L * 1000);
    }

    public Date getNewLimitedRefreshExpirationDate(LocalDateTime limit) {
        if(limit != null) {
            long millis = Duration.between(LocalDateTime.now(), limit).toMillis();
            if(millis > 0) {
                return new Date(System.currentTimeMillis() + Math.min(jwtConfig.getJwtRefreshTokenExpirationInMinutes() * 60L * 1000, millis));
            }
            else {
                return null;
            }
        }
        else {
            return getNewRefreshExpirationDate();
        }
    }

    public Date getNewLimitedAccessExpirationDate(LocalDateTime limit) {
        if(limit != null) {
            long millis = Duration.between(LocalDateTime.now(), limit).toMillis();
            if(millis > 0) {
                return new Date(System.currentTimeMillis() + Math.min(jwtConfig.getJwtAccessTokenExpirationInMinutes() * 60L * 1000, millis));
            }
            else {
                return null;
            }
        }
        else {
            return getNewAccessExpirationDate();
        }
    }

    public String generateAccessToken(String username, Date expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_CLAIM_KEY_TYPE, TOKEN_CLAIM_VALUE_TYPE_ACCESS);
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(expiration)
                .signWith(getSecretKey())
                .compact();
    }

    public String generateRefreshToken(String username, Date expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_CLAIM_KEY_TYPE, TOKEN_CLAIM_VALUE_TYPE_REFRESH);
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(expiration)
                .signWith(getSecretKey())
                .compact();
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token, true).before(new Date());
        }
        catch (ExpiredJwtException ignored) {
            return true;
        }
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token, true);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
