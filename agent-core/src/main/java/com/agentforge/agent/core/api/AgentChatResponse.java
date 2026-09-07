package com.agentforge.agent.core.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

/**
 * Agent 问答响应体，由 {@link com.agentforge.agent.runtime.AgentOrchestrator} 组装后返回客户端。
 *
 * @param traceId         链路 ID，与日志、agent_sessions、rag_hit_logs 对齐
 * @param question        本轮用户问题（回显）
 * @param answer          LLM 或澄清策略生成的回答
 * @param durationMs      本轮处理耗时（毫秒）
 * @param toolCalls       本次调用的工具名列表（或降级说明）
 * @param ragHits         RAG 命中摘要（兼容旧前端的字符串列表）
 * @param runtime         实际运行路径，如 manual / LangChain4j / agentscope fallback -> manual
 * @param sessionId       会话 ID，客户端下一轮应原样传回
 * @param turnIndex       当前轮次序号
 * @param pendingActions  本 session 待人工确认的 HITL 写操作
 * @param ragHitDetails   RAG 命中结构化详情（控制台观测优先使用）
 */
public record AgentChatResponse(
    String traceId,
    String question,
    String answer,
    long durationMs,
    List<String> toolCalls,
    List<String> ragHits,
    @JsonAlias("agentScopeRuntime") String runtime,
    String sessionId,
    int turnIndex,
    List<AgentPendingActionView> pendingActions,
    List<AgentRagHitView> ragHitDetails
) {
    /**
     * 兼容旧版响应：不含会话、HITL 与结构化 RAG。
     */
    public AgentChatResponse(String traceId, String question, String answer, long durationMs,
                             List<String> toolCalls, List<String> ragHits, String runtime) {
        this(traceId, question, answer, durationMs, toolCalls, ragHits, runtime,
            null, 0, List.of(), List.of());
    }

    /**
     * 兼容不含结构化 RAG 的完整响应构造。
     */
    public AgentChatResponse(String traceId, String question, String answer, long durationMs,
                             List<String> toolCalls, List<String> ragHits, String runtime,
                             String sessionId, int turnIndex,
                             List<AgentPendingActionView> pendingActions) {
        this(traceId, question, answer, durationMs, toolCalls, ragHits, runtime,
            sessionId, turnIndex, pendingActions, List.of());
    }
}
