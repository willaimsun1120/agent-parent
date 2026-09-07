package com.agentforge.agent.core.api;

/**
 * RAG 命中详情，供控制台观测区展示 docCode / 分数 / 片段。
 *
 * @param docCode 文档编码
 * @param title   标题
 * @param score   相似度分数
 * @param snippet 内容片段（截断）
 */
public record AgentRagHitView(
    String docCode,
    String title,
    double score,
    String snippet
) {
    /**
     * 生成兼容旧版的单行摘要。
     */
    public String summaryLine() {
        String head = (docCode == null || docCode.isBlank())
            ? (title == null ? "" : title)
            : docCode + (title == null || title.isBlank() ? "" : " · " + title);
        return head + " score=" + String.format("%.4f", score);
    }
}
