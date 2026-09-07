package com.agentforge.agent.core.api;

/**
 * Agent 问答请求体（POST /api/agent/chat）。
 *
 * @param question  用户问题
 * @param mode      运行模式：manual / langchain4j / agentscope，空则默认 manual
 * @param sessionId 多轮会话 ID，空则服务端生成
 * @param category  可选 RAG 过滤分类，空则由 RagCategoryResolver 从问题推断
 */
public record AgentChatRequest(String question, String mode, String sessionId, String category) {

    /**
     * 简化构造：不指定会话与 RAG 分类，由服务端生成 sessionId 并自动推断 category。
     *
     * @param question 用户问题
     * @param mode     运行模式
     */
    public AgentChatRequest(String question, String mode) {
        this(question, mode, null, null);
    }
}
