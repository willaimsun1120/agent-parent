package com.agentforge.agent.core.spi;

import java.util.Optional;

/**
 * 澄清策略评估上下文，由 {@link com.agentforge.agent.clarification.ClarificationService} 组装。
 *
 * @param question  用户问题
 * @param sessionId 会话 ID
 * @param turnIndex 当前轮次
 * @param category  RAG 分类，可选
 */
public record ClarificationContext(
    String question,
    String sessionId,
    int turnIndex,
    Optional<String> category
) {
}
