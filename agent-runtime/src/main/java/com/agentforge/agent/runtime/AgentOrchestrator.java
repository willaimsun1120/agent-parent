package com.agentforge.agent.runtime;

import com.agentforge.agent.core.api.AgentChatRequest;
import com.agentforge.agent.core.api.AgentChatResponse;
import com.agentforge.agent.core.mode.AgentMode;
import com.agentforge.agent.core.mode.AgentModeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Agent 编排门面（平台调度中枢）。
 *
 * <p>职责单一：解析请求中的 {@code mode}，从 {@link AgentModeRegistry} 取出对应 Handler 并委托执行。
 * 不包含澄清、RAG、工具调用等业务逻辑——这些在各 {@link AgentModeHandler} 及 SPI 中完成。
 *
 * <p>典型调用链：
 * <pre>
 * AgentController.chat()
 *   → AgentOrchestrator.chat()          // 本类：选 mode
 *   → ManualAgentModeHandler.chat()     // 或 langchain4j / agentscope
 *       → ClarificationService          // 澄清链
 *       → ManualAgentExecutor (SPI)     // 业务编排
 *       → AgentExecutionSupport         // 会话 / 落库 / 组装响应
 * </pre>
 */
@Service
public class AgentOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final AgentModeRegistry modeRegistry;

    /**
     * @param modeRegistry 模式 Handler 注册表
     */
    public AgentOrchestrator(AgentModeRegistry modeRegistry) {
        this.modeRegistry = modeRegistry;
    }

    /**
     * 按请求中的 mode 路由到对应 Handler 并执行问答。
     *
     * @param request 客户端问答请求
     * @return 问答响应
     */
    public AgentChatResponse chat(AgentChatRequest request) {
        AgentMode mode = AgentMode.from(request == null ? null : request.mode());
        String sessionId = request == null ? null : request.sessionId();
        log.info("AgentOrchestrator 收到 chat 请求 mode={} sessionId={} questionLength={}",
            mode.code(), sessionId, request == null || request.question() == null ? 0 : request.question().length());
        AgentModeHandler handler = modeRegistry.get(mode);
        log.debug("AgentOrchestrator 选择 handler={} mode={}", handler.getClass().getSimpleName(), mode.code());
        AgentChatResponse response = handler.chat(request);
        log.info("AgentOrchestrator chat 完成 mode={} traceId={} durationMs={} pendingActions={}",
            mode.code(), response.traceId(), response.durationMs(),
            response.pendingActions() == null ? 0 : response.pendingActions().size());
        return response;
    }
}
