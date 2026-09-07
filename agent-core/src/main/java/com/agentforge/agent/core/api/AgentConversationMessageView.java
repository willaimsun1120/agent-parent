package com.agentforge.agent.core.api;

/**
 * 会话历史消息视图，供控制台刷新后恢复展示与无限滚动分页。
 *
 * @param id        消息主键（分页游标）
 * @param role      user / assistant
 * @param content   消息正文
 * @param turnIndex 轮次序号
 * @param traceId   关联 trace
 * @param createdAt 消息时间（ISO-8601，如 2026-09-04T17:46:00）
 */
public record AgentConversationMessageView(
    Long id,
    String role,
    String content,
    Integer turnIndex,
    String traceId,
    String createdAt
) {
}
