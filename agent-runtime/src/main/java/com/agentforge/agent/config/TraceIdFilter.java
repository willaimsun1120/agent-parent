package com.agentforge.agent.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 全链路 traceId 注入过滤器。
 *
 * <p>位于 agent-runtime 最外层横切逻辑：从 {@code X-Trace-Id} 请求头读取或生成 traceId，
 * 写入 MDC 与响应头，供日志、会话表、工具日志等模块串联同一次 HTTP 请求。
 */
@Component
@Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    /** MDC 中 traceId 的键名，与 {@link com.agentforge.agent.runtime.AgentExecutionSupport} 一致 */
    public static final String TRACE_ID = "traceId";

    /**
     * 解析/生成 traceId，写入 MDC 与响应头，并记录请求起止日志。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(TRACE_ID, traceId);
        response.setHeader("X-Trace-Id", traceId);
        long started = System.currentTimeMillis();
        try {
            // 关键链路：每个 HTTP 请求都会打上 traceId，后续日志、工具调用和会话表都用它串联。
            log.info("HTTP 请求开始 method={} uri={}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
        } finally {
            log.info("HTTP 请求结束 method={} uri={} status={} durationMs={}",
                request.getMethod(), request.getRequestURI(), response.getStatus(), System.currentTimeMillis() - started);
            MDC.remove(TRACE_ID);
        }
    }
}
