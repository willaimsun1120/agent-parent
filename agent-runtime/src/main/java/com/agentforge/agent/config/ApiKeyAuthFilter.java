package com.agentforge.agent.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * API Key 鉴权 Servlet 过滤器。
 *
 * <p>位于 agent-runtime 安全层，在 TraceIdFilter 之后执行。
 * 对除 actuator、admin、根路径、error 外的请求校验 {@code X-API-Key} 请求头；
 * 未配置鉴权或路径在白名单内时跳过。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiKeyProperties properties;

    /**
     * @param properties API Key 配置
     */
    public ApiKeyAuthFilter(ApiKeyProperties properties) {
        this.properties = properties;
    }

    /**
     * 未启用鉴权或路径在白名单内时不执行过滤。
     *
     * @param request HTTP 请求
     * @return 跳过时为 true
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.requiresAuth()) {
            return true;
        }
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
            || path.startsWith("/admin")
            || path.equals("/")
            || path.startsWith("/error");
    }

    /**
     * 校验 X-API-Key，不匹配则返回 401 JSON。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String provided = request.getHeader("X-API-Key");
        if (properties.apiKey().equals(provided)) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Missing or invalid X-API-Key\"}");
    }
}
