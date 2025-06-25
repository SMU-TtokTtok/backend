package org.project.ttokttok.infrastructure.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.project.ttokttok.global.entity.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import java.security.Key;
import java.util.Date;

import static org.project.ttokttok.global.auth.jwt.TokenExpiry.ACCESS_TOKEN_EXPIRY_TIME;

@Component
@ActiveProfiles("test")
public class JwtFactory {

    @Value("${jwt.issuer}")
    private String issuer;

    private Key key;

    public JwtFactory(@Value("${jwt.secret}") String secret) {
        // 시크릿값 base64 디코딩
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateValidToken(String username, Role role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_EXPIRY_TIME.getExpiry());

        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("role", role.toString())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateExpiredToken(String username, Role role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() - ACCESS_TOKEN_EXPIRY_TIME.getExpiry());

        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("role", role.toString())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateInvalidIssuerToken(String username, Role role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_EXPIRY_TIME.getExpiry());

        return Jwts.builder()
                .setIssuer("sumin")
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("role", role.toString())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
