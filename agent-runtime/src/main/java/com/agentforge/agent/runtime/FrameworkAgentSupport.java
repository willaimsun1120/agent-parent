package com.agentforge.agent.runtime;

import java.util.List;

/**
 * 框架模式（AgentScope / LangChain4j）共用的工具调用与 RAG 提示逻辑。
 *
 * <p>位于 agent-runtime 层，为各框架 ModeHandler 提供统一的工具/RAG 摘要文案与空跑检测，
 * 避免在 agent-mode-* 模块中重复实现。
 */
public final class FrameworkAgentSupport {

    private FrameworkAgentSupport() {
    }

    /**
     * 生成框架模式下默认工具调用提示文案。
     *
     * @param frameworkName 框架名称（如 LangChain4j、AgentScope）
     * @return 单行提示列表
     */
    public static List<String> defaultToolCallHint(String frameworkName) {
        return List.of(frameworkName + " 自动选择工具：get_order_detail/get_payment_status/get_refund_status/get_benefit_status/search_knowledge/submit_refund_request/retry_benefit_issue");
    }

    /**
     * 根据是否触发 search_knowledge 生成 RAG 命中提示。
     *
     * @param toolCalls 本次工具调用摘要
     * @param frameworkName 框架名称
     * @return RAG 相关提示列表
     */
    public static List<String> ragHitHints(List<String> toolCalls, String frameworkName) {
        boolean searchedKnowledge = toolCalls.stream().anyMatch(call -> call.startsWith("search_knowledge"));
        if (searchedKnowledge) {
            return List.of("RAG 已由 " + frameworkName + " 工具 search_knowledge 按需检索，详细命中可看 rag_hit_logs 表");
        }
        return List.of("本次 " + frameworkName + " 推理未触发 search_knowledge，建议检查 prompt 或模型工具选择");
    }

    /**
     * 问题是否应触发至少一次工具调用（用于检测模型「空跑」）。
     *
     * @param question 用户问题
     * @return 含订单号或业务关键词时为 true
     */
    public static boolean expectsToolCalls(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        return question.matches(".*ORD-\\d+.*")
            || question.contains("支付")
            || question.contains("退款")
            || question.contains("权益")
            || question.contains("会员");
    }
}
