package com.agentforge.agent.runtime;

import com.agentforge.agent.core.api.AgentChatRequest;
import com.agentforge.agent.core.api.AgentChatResponse;
import com.agentforge.agent.core.api.AgentRagHitView;
import com.agentforge.agent.core.mode.AgentMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 统一 SSE 流式输出：三模式共用「先完整执行，再按块推送 delta」策略，保证行为一致。
 *
 * <p>不依赖 {@code @RequestScope}：SSE 返回后 HTTP request scope 会失效，
 * 改由 {@link AgentRequestContextHolder} 在异步线程承载会话上下文。
 */
@Service
public class AgentChatStreamService {
    private static final Logger log = LoggerFactory.getLogger(AgentChatStreamService.class);
    private static final long TIMEOUT_MS = 180_000L;
    private static final int CHUNK_SIZE = 8;
    private static final long CHUNK_DELAY_MS = 28L;

    private final AgentOrchestrator orchestrator;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "agent-chat-stream");
        t.setDaemon(true);
        return t;
    });

    public AgentChatStreamService(AgentOrchestrator orchestrator, ObjectMapper objectMapper) {
        this.orchestrator = orchestrator;
        this.objectMapper = objectMapper;
    }

    /**
     * 启动统一流式会话。
     */
    public SseEmitter stream(AgentChatRequest request) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        executor.execute(() -> {
            try {
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                } else {
                    MDC.clear();
                }
                runStream(emitter, request);
            } finally {
                AgentRequestContextHolder.clear();
                MDC.clear();
            }
        });
        return emitter;
    }

    private void runStream(SseEmitter emitter, AgentChatRequest request) {
        AgentMode mode = AgentMode.from(request == null ? null : request.mode());
        try {
            send(emitter, "meta", Map.of(
                "mode", mode.code(),
                "phase", "started",
                "status", "模型推理中，请稍候…"
            ));

            AgentChatResponse response = orchestrator.chat(request);

            Map<String, Object> ready = new LinkedHashMap<>();
            ready.put("phase", "ready");
            ready.put("status", "开始流式输出…");
            ready.put("traceId", response.traceId());
            ready.put("sessionId", response.sessionId());
            ready.put("turnIndex", response.turnIndex());
            ready.put("runtime", response.runtime());
            ready.put("mode", mode.code());
            send(emitter, "meta", ready);

            List<String> toolCalls = response.toolCalls() == null ? List.of() : response.toolCalls();
            for (String toolCall : toolCalls) {
                send(emitter, "tool", Map.of("raw", toolCall));
            }

            List<AgentRagHitView> details = response.ragHitDetails() == null
                ? List.of() : response.ragHitDetails();
            if (!details.isEmpty()) {
                for (AgentRagHitView hit : details) {
                    send(emitter, "rag", hit);
                }
            } else if (response.ragHits() != null) {
                for (String hit : response.ragHits()) {
                    send(emitter, "rag", Map.of("summary", hit));
                }
            }

            String answer = response.answer() == null ? "" : response.answer();
            for (int i = 0; i < answer.length(); i += CHUNK_SIZE) {
                int end = Math.min(i + CHUNK_SIZE, answer.length());
                send(emitter, "delta", Map.of("text", answer.substring(i, end)));
                try {
                    Thread.sleep(CHUNK_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            send(emitter, "done", response);
            emitter.complete();
            log.info("SSE chat stream 完成 mode={} traceId={} answerLength={}",
                mode.code(), response.traceId(), answer.length());
        } catch (Exception ex) {
            log.warn("SSE chat stream 失败 mode={} message={}", mode.code(), ex.getMessage(), ex);
            try {
                send(emitter, "error", Map.of(
                    "message", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()
                ));
                emitter.complete();
            } catch (Exception sendEx) {
                emitter.completeWithError(ex);
            }
        }
    }

    private void send(SseEmitter emitter, String event, Object data) throws IOException {
        // 用纯文本 JSON，避免部分环境下 APPLICATION_JSON 二次包装导致前端解析失败
        String json = objectMapper.writeValueAsString(data);
        emitter.send(SseEmitter.event()
            .name(event)
            .data(json, MediaType.TEXT_PLAIN));
    }
}
