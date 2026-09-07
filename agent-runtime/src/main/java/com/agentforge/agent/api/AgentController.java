package com.agentforge.agent.api;

import com.agentforge.agent.conversation.AgentConversationService;
import com.agentforge.agent.core.api.AgentChatRequest;
import com.agentforge.agent.core.api.AgentChatResponse;
import com.agentforge.agent.core.api.AgentConversationPageView;
import com.agentforge.agent.runtime.AgentChatStreamService;
import com.agentforge.agent.runtime.AgentOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent 问答 HTTP 入口。
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentOrchestrator orchestrator;
    private final AgentChatStreamService chatStreamService;
    private final AgentConversationService conversationService;

    public AgentController(AgentOrchestrator orchestrator,
                           AgentChatStreamService chatStreamService,
                           AgentConversationService conversationService) {
        this.orchestrator = orchestrator;
        this.chatStreamService = chatStreamService;
        this.conversationService = conversationService;
    }

    @PostMapping("/chat")
    public AgentChatResponse chat(@RequestBody AgentChatRequest request) {
        long started = System.currentTimeMillis();
        String question = request == null ? "" : request.question();
        log.info("收到 Agent 问答请求 questionLength={}", question == null ? 0 : question.length());
        try {
            AgentChatResponse response = orchestrator.chat(request);
            log.info("Agent 问答请求完成 traceId={} durationMs={}",
                response.traceId(), System.currentTimeMillis() - started);
            return response;
        } catch (RuntimeException ex) {
            log.warn("Agent 问答请求失败 durationMs={} message={}",
                System.currentTimeMillis() - started, ex.getMessage());
            throw ex;
        }
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody AgentChatRequest request,
                                 jakarta.servlet.http.HttpServletResponse response) {
        String question = request == null ? "" : request.question();
        log.info("收到 Agent 流式问答请求 questionLength={}", question == null ? 0 : question.length());
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");
        return chatStreamService.stream(request);
    }

    /**
     * 按 sessionId 分页拉取对话历史。
     * <p>默认返回最新一页；传 {@code beforeId} 继续向上加载更早消息（无限滚动）。
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public AgentConversationPageView listMessages(@PathVariable String sessionId,
                                                  @RequestParam(required = false) Long beforeId,
                                                  @RequestParam(defaultValue = "20") int limit) {
        log.info("查询会话消息 sessionId={} beforeId={} limit={}", sessionId, beforeId, limit);
        return conversationService.pageTurns(sessionId, beforeId, limit);
    }
}
