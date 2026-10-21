package com.familymemory.security;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT请求认证过滤器，认证完成后维护并清理当前线程用户上下文。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    /**
     * JWT签发和验签服务，令牌主题保存用户ID。
     */
    private final JwtService jwtService;

    /**
     * 初始化 JwtAuthenticationFilter 的依赖。
     * @param jwtService JWT签发和验签服务，令牌主题保存用户ID
     */
    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * 解析Bearer令牌、设置当前用户上下文，并在请求结束后清理。
     * @param request 当前HTTP请求，可包含Authorization Bearer令牌
     * @param response 当前HTTP响应
     * @param chain 后续Servlet过滤器链
     * @throws ServletException 请求处理或安全配置失败
     * @throws IOException 请求或响应流处理失败
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                UserContext.set(jwtService.parse(authorization.substring(7)));
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"登录状态已失效\",\"data\":null}");
                return;
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
