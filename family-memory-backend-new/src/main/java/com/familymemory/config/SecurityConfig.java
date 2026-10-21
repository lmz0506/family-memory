package com.familymemory.config;

import com.familymemory.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 接口安全配置，定义密码编码、公开路径和JWT认证过滤链。
 */
@Configuration
public class SecurityConfig {
    /**
     * 创建BCrypt密码编码器。
     * @return BCrypt密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置公开接口、JWT过滤器和无状态安全过滤链。
     * @param http Spring Security安全配置构建器
     * @param filter JWT认证过滤器
     * @return 已配置的安全过滤链
     * @throws Exception 请求处理或安全配置失败
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter filter) throws Exception {
        http.csrf().disable().cors().and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests().antMatchers("/auth/login", "/auth/register", "/files/**", "/actuator/health").permitAll()
                .anyRequest().permitAll()
                .and().addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
