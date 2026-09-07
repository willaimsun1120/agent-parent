package com.agentforge.agent.observability;

import java.util.List;

/**
 * 按 traceId 聚合的观测详情，供控制台点击历史消息回放。
 *
 * @param traceId       链路 ID
 * @param sessionId     会话 ID
 * @param turnIndex     轮次
 * @param durationMs    耗时
 * @param toolCalls     工具调用摘要（name(args)）
 * @param ragHitDetails RAG 命中详情
 */
public record ObservabilityTraceView(
    String traceId,
    String sessionId,
    Integer turnIndex,
    Long durationMs,
    List<String> toolCalls,
    List<RagHitDetailView> ragHitDetails
) {
    /**
     * @param docCode 文档编码 / source
     * @param title   标题（可空）
     * @param score   分数
     * @param snippet 片段
     */
    public record RagHitDetailView(
        String docCode,
        String title,
        Double score,
        String snippet
    ) {
    }
}
