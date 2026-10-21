package com.familymemory.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * JWT签发和验签服务，令牌主题保存用户ID。
 */
@Component
public class JwtService {
    /**
     * JWT签名密钥，仅在服务端持有。
     */
    private final SecretKey key;
    /**
     * JWT有效小时数。
     */
    private final long expirationHours;

    /**
     * 初始化 JwtService 的依赖。
     * @param secret JWT签名密钥配置app.jwt.secret，不得输出明文
     * @param expirationHours 令牌有效小时数
     */
    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-hours}") long expirationHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationHours = expirationHours;
    }

    /**
     * 以用户ID为主题签发带有效期的JWT。
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 已签名的JWT字符串
     */
    public String create(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder().setSubject(String.valueOf(userId)).setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(Duration.ofHours(expirationHours)))).signWith(key).compact();
    }

    /**
     * 校验JWT签名与有效期并提取用户ID。
     * @param token 待验证的JWT令牌
     * @return 已验签令牌中的用户ID，来源表：fm_user.id
     */
    public Long parse(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return Long.valueOf(claims.getSubject());
    }
}
