package com.agentforge.agent.config;

import com.agentforge.agent.core.exception.AgentErrorCode;
import com.agentforge.agent.core.exception.AgentException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * 全局异常处理器，将 {@link AgentException} 和 {@link ResponseStatusException} 映射为统一 JSON 响应。
 *
 * <p>响应体格式：
 * <pre>{@code
 * {
 *   "timestamp": "2026-06-12T10:00:00Z",
 *   "status": 404,
 *   "error": "Not Found",
 *   "code": "ACTION_NOT_FOUND",
 *   "message": "Action not found: act-xxx"
 * }
 * }</pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理平台统一异常 {@link AgentException}，按 {@link AgentErrorCode} 映射 HTTP 状态。
     *
     * @param ex Agent 平台异常
     * @return 统一错误响应
     */
    @ExceptionHandler(AgentException.class)
    public ResponseEntity<Map<String, Object>> handleAgentException(AgentException ex) {
        HttpStatus status = mapErrorCode(ex.errorCode());
        log.warn("AgentException code={} message={}", ex.errorCode(), ex.getMessage());
        return ResponseEntity.status(status).body(buildBody(status, ex.errorCode().name(), ex.getMessage()));
    }

    /**
     * 处理 Spring WebFlux/WebMVC 的 {@link ResponseStatusException}。
     *
     * @param ex ResponseStatus 异常
     * @return 统一错误响应
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        log.warn("ResponseStatusException status={} reason={}", status, ex.getReason());
        return ResponseEntity.status(status).body(buildBody(status, null, ex.getReason()));
    }

    /**
     * 兜底处理未捕获的运行时异常。
     *
     * @param ex 运行时异常
     * @return 500 统一错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        // SSE 已提交 text/event-stream 时不能再写 JSON，避免二次异常刷屏
        if (ex instanceof org.springframework.http.converter.HttpMessageNotWritableException) {
            log.warn("跳过已提交 SSE 响应上的异常包装: {}", ex.getMessage());
            return null;
        }
        log.error("未捕获的运行时异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(buildBody(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "服务内部错误，请稍后重试"));
    }

    private HttpStatus mapErrorCode(AgentErrorCode code) {
        return switch (code) {
            case MODE_NOT_REGISTERED -> HttpStatus.BAD_REQUEST;
            case CLARIFICATION_REQUIRED -> HttpStatus.BAD_REQUEST;
            case ACTION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ACTION_NOT_PENDING -> HttpStatus.CONFLICT;
            case ACTION_UNSUPPORTED -> HttpStatus.BAD_REQUEST;
            case ACTION_CONFLICT -> HttpStatus.CONFLICT;
            case EXECUTION_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private Map<String, Object> buildBody(HttpStatus status, String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        if (code != null) {
            body.put("code", code);
        }
        body.put("message", message == null ? "" : message);
        return body;
    }
}
