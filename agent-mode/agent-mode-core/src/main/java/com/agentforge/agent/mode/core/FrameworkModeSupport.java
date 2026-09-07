package com.agentforge.agent.mode.core;

import com.agentforge.agent.action.AgentActionService;
import com.agentforge.agent.clarification.ClarificationService;
import com.agentforge.agent.core.api.AgentChatRequest;
import com.agentforge.agent.core.api.AgentChatResponse;
import com.agentforge.agent.core.api.AgentPendingActionView;
import com.agentforge.agent.core.spi.ClarificationContext;
import com.agentforge.agent.core.spi.ClarificationResult;
import com.agentforge.agent.runtime.AgentExecutionSupport;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 框架模式（LangChain4j / AgentScope）公共执行骨架。
 *
 * <p>提取两种框架模式在 API Key 检查、澄清链、降级、响应组装等环节的重复逻辑，
 * 子类只需实现 {@link #invokeFramework} 提供框架特定的推理调用。
 *
 * <p>通过 {@link FallbackChatSupport} 接口解耦对 manual 模式的编译期依赖，
 * 运行时由 Spring 注入 {@code ManualAgentModeHandler} 实例。
 */
public abstract class FrameworkModeSupport {
    private static final Logger log = LoggerFactory.getLogger(FrameworkModeSupport.class);

    protected final AgentExecutionSupport executionSupport;
    protected final AgentActionService actionService;
    protected final FallbackChatSupport fallbackChat;
    protected final AgentFallbackSupport fallbackSupport;
    protected final ClarificationService clarificationService;

    protected FrameworkModeSupport(AgentExecutionSupport executionSupport,
                                   AgentActionService actionService,
                                   FallbackChatSupport fallbackChat,
                                   AgentFallbackSupport fallbackSupport,
                                   ClarificationService clarificationService) {
        this.executionSupport = executionSupport;
        this.actionService = actionService;
        this.fallbackChat = fallbackChat;
        this.fallbackSupport = fallbackSupport;
        this.clarificationService = clarificationService;
    }

    /**
     * 框架模式通用执行流程：API Key 检查 → 准备上下文 → 澄清 → 框架推理 → 组装响应。
     *
     * @param request       对话请求
     * @param frameworkName 框架名称（如 "LangChain4j"、"AgentScope"）
     * @param enabled       API Key 是否已配置
     * @return 对话响应
     */
    protected AgentChatResponse doChat(AgentChatRequest request, String frameworkName, boolean enabled) {
        if (!enabled) {
            log.info("未配置 DASHSCOPE_API_KEY，{} 模式降级为手写模式", frameworkName);
            return fallbackSupport.fallbackToManual(fallbackChat, request,
                frameworkName + " 未配置 API Key，已降级到手写模式", frameworkName);
        }

        long started = System.currentTimeMillis();
        executionSupport.beginChatMetrics();
        AgentExecutionSupport.PreparedChat prepared = executionSupport.prepareChat(request, frameworkName.toLowerCase(), started);
        String traceId = prepared.traceId();
        String question = prepared.question();

        Optional<ClarificationResult> clarification = clarificationService.evaluate(new ClarificationContext(
            question, prepared.sessionId(), prepared.turnIndex(), prepared.category()));
        if (clarification.isPresent()) {
            return finishClarification(prepared, started, clarification.get(), frameworkName);
        }

        log.info("开始处理 {} 模式问题 traceId={} questionLength={}", frameworkName, traceId, question.length());
        try {
            return invokeFramework(prepared, started, frameworkName);
        } catch (RuntimeException ex) {
            log.warn("{} 模式调用失败，降级为手写模式 traceId={} message={}", frameworkName, traceId, ex.getMessage());
            return fallbackSupport.fallbackToManual(fallbackChat, request,
                frameworkName + " 调用失败，已降级到手写模式：" + ex.getMessage(), frameworkName);
        }
    }

    /**
     * 子类实现：执行框架特定的推理逻辑（构建 Agent、调用模型、收集工具调用等）。
     *
     * @param prepared      预处理上下文
     * @param started       开始时间戳
     * @param frameworkName 框架名称
     * @return 对话响应
     */
    protected abstract AgentChatResponse invokeFramework(AgentExecutionSupport.PreparedChat prepared,
                                                          long started, String frameworkName);

    /**
     * 澄清链命中时直接返回追问，跳过框架推理。
     */
    protected AgentChatResponse finishClarification(AgentExecutionSupport.PreparedChat prepared, long started,
                                                     ClarificationResult clarification, String frameworkName) {
        long durationMs = System.currentTimeMillis() - started;
        String answer = clarification.message();
        executionSupport.recordChatDuration(durationMs);
        executionSupport.saveSession(prepared.traceId(), prepared.sessionId(), prepared.turnIndex(),
            prepared.question(), answer, durationMs);
        return executionSupport.buildResponse(prepared.traceId(), prepared.sessionId(), prepared.turnIndex(),
            prepared.question(), answer, durationMs, List.of(), List.of(),
            frameworkName + " clarification", List.of());
    }

    /**
     * 框架推理成功后的通用响应组装：落库 + 构建 DTO。
     */
    protected AgentChatResponse buildFrameworkResponse(AgentExecutionSupport.PreparedChat prepared,
                                                        long started, String answer,
                                                        List<String> toolCalls, List<String> ragHits,
                                                        String runtimeInfo) {
        long durationMs = System.currentTimeMillis() - started;
        executionSupport.recordChatDuration(durationMs);
        executionSupport.saveSession(prepared.traceId(), prepared.sessionId(), prepared.turnIndex(),
            prepared.question(), answer, durationMs);
        List<AgentPendingActionView> pending = actionService.listPending(prepared.sessionId());
        return executionSupport.buildResponse(prepared.traceId(), prepared.sessionId(), prepared.turnIndex(),
            prepared.question(), answer, durationMs, toolCalls, ragHits, runtimeInfo, pending);
    }
}
