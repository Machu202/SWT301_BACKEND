// Vị trí: src/main/java/com/swt301/ecommerce/security/JwtUtils.java
package com.swt301.ecommerce.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret:MotChuoiBiMatSieuDaiVaPhucTapDeBaoMatHon256BitsNhe}")
    private String jwtSecret;

    @Value("${jwt.expirationMs:86400000}") // 24h
    private int jwtExpirationMs;

    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject((userPrincipal.getUsername()))
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key key() {
        // Truyền thẳng mảng byte[] vào hàm tạo Key
        return Keys.hmacShaKeyFor(tryGetBase64Secret());
    }
    
    private byte[] tryGetBase64Secret() {
        try {
            // Thử giải mã nếu chuỗi secret của bạn trong application.yml là dạng Base64
            return Decoders.BASE64.decode(jwtSecret);
        } catch (Exception e) {
            // Nếu không phải dạng Base64 thì cứ lấy mảng byte bình thường
            return jwtSecret.getBytes();
        }
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parse(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}