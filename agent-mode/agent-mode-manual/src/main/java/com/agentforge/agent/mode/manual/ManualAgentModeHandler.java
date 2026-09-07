package com.agentforge.agent.mode.manual;

import com.agentforge.agent.action.AgentActionService;
import com.agentforge.agent.clarification.ClarificationService;
import com.agentforge.agent.core.api.AgentChatRequest;
import com.agentforge.agent.core.api.AgentChatResponse;
import com.agentforge.agent.core.api.AgentPendingActionView;
import com.agentforge.agent.core.mode.AgentMode;
import com.agentforge.agent.core.mode.AgentModeHandler;
import com.agentforge.agent.core.spi.ClarificationContext;
import com.agentforge.agent.core.spi.ClarificationResult;
import com.agentforge.agent.core.spi.ManualAgentExecutor;
import com.agentforge.agent.mode.core.FallbackChatSupport;
import com.agentforge.agent.runtime.AgentExecutionSupport;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * manual 模式处理器：平台通用骨架 + 业务 SPI。
 *
 * <p>执行顺序（与框架模式前半段一致）：
 * <ol>
 *   <li>{@link com.agentforge.agent.runtime.AgentExecutionSupport#prepareChat} 准备上下文</li>
 *   <li>{@link com.agentforge.agent.clarification.ClarificationService} 澄清链（缺订单号等则直接返回追问）</li>
 *   <li>{@link ManualAgentExecutor} SPI — 业务实现关键词/规则编排、调工具、调 RAG、调 LLM</li>
 *   <li>落库 + 附带当前 session 的 HITL 待确认列表</li>
 * </ol>
 *
 * <p>{@link #chatAsFallback} 供 LangChain4j / AgentScope 降级时调用，跳过 metrics 重复计数。
 */
@Service
public class ManualAgentModeHandler implements AgentModeHandler, FallbackChatSupport {
    private static final Logger log = LoggerFactory.getLogger(ManualAgentModeHandler.class);

    private final ManualAgentExecutor executor;
    private final ClarificationService clarificationService;
    private final AgentExecutionSupport executionSupport;
    private final AgentActionService actionService;

    /**
     * 构造手写模式处理器。
     *
     * @param executor            业务编排 SPI
     * @param clarificationService 澄清链服务
     * @param executionSupport    会话与指标支撑
     * @param actionService       HITL 待确认动作服务
     */
    public ManualAgentModeHandler(ManualAgentExecutor executor,
                                  ClarificationService clarificationService,
                                  AgentExecutionSupport executionSupport,
                                  AgentActionService actionService) {
        this.executor = executor;
        this.clarificationService = clarificationService;
        this.executionSupport = executionSupport;
        this.actionService = actionService;
    }

    /** {@inheritDoc} */
    @Override
    public AgentMode mode() {
        return AgentMode.MANUAL;
    }

    /**
     * 处理手写模式对话请求，计入平台指标。
     *
     * @param request 对话请求
     * @return 对话响应
     */
    @Override
    public AgentChatResponse chat(AgentChatRequest request) {
        return execute(request, true);
    }

    /** 供框架模式降级调用，避免重复计数 metrics */
    public AgentChatResponse chatAsFallback(AgentChatRequest request) {
        return execute(request, false);
    }

    /**
     * 手写模式核心执行流程：准备上下文 → 澄清 → 业务编排 → 落库。
     *
     * @param request        对话请求
     * @param recordMetrics  是否计入 chat 指标（降级调用时为 false）
     * @return 对话响应
     */
    private AgentChatResponse execute(AgentChatRequest request, boolean recordMetrics) {
        long started = System.currentTimeMillis();
        if (recordMetrics) {
            executionSupport.beginChatMetrics();
        }
        AgentExecutionSupport.PreparedChat prepared = executionSupport.prepareChat(request, "manual", started);
        log.info("开始处理手写模式问题 traceId={} sessionId={} questionLength={}",
            prepared.traceId(), prepared.sessionId(), prepared.question().length());

        // ① 澄清：业务 ClarificationPolicy 判断是否需要追问（如缺订单号）
        Optional<ClarificationResult> clarification = clarificationService.evaluate(new ClarificationContext(
            prepared.question(), prepared.sessionId(), prepared.turnIndex(), prepared.category()));
        if (clarification.isPresent()) {
            return finishClarification(prepared, started, clarification.get(), recordMetrics);
        }

        // ② 业务编排：订单 Demo 中为 OrderManualAgentExecutor（抽订单号 → 查库 → RAG → LLM）
        ManualAgentExecutor.ManualExecutionResult result = executor.execute(new ManualAgentExecutor.ManualExecutionContext(
            request, prepared.traceId(), prepared.sessionId(), prepared.turnIndex(),
            prepared.question(), prepared.history(), prepared.category(), recordMetrics));

        long durationMs = System.currentTimeMillis() - started;
        if (recordMetrics) {
            executionSupport.recordChatDuration(durationMs);
        }
        AgentChatResponse response = result.response();
        executionSupport.saveSession(prepared.traceId(), prepared.sessionId(), prepared.turnIndex(),
            prepared.question(), response.answer(), durationMs);
        List<AgentPendingActionView> pending = actionService.listPending(prepared.sessionId());
        log.info("手写模式处理完成 traceId={} durationMs={} pendingActions={}",
            prepared.traceId(), durationMs, pending.size());
        return executionSupport.buildResponse(
            prepared.traceId(), prepared.sessionId(), prepared.turnIndex(),
            prepared.question(), response.answer(), durationMs,
            result.toolCalls(), result.ragHits(), response.runtime(), pending,
            response.ragHitDetails());
    }

    /** 澄清链命中时直接返回追问，跳过业务编排。 */
    private AgentChatResponse finishClarification(AgentExecutionSupport.PreparedChat prepared, long started,
                                                    ClarificationResult clarification, boolean recordMetrics) {
        String answer = clarification.message();
        long durationMs = System.currentTimeMillis() - started;
        if (recordMetrics) {
            executionSupport.recordChatDuration(durationMs);
        }
        executionSupport.saveSession(prepared.traceId(), prepared.sessionId(), prepared.turnIndex(),
            prepared.question(), answer, durationMs);
        log.info("手写模式澄清返回 traceId={} reason={}", prepared.traceId(), clarification.reason());
        return executionSupport.buildResponse(prepared.traceId(), prepared.sessionId(), prepared.turnIndex(),
            prepared.question(), answer, durationMs, List.of(), List.of(), "manual-clarification", List.of());
    }
}
