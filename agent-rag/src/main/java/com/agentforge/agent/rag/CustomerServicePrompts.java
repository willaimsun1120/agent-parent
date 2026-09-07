package com.agentforge.agent.rag;

import java.util.List;

/**
 * 客服场景 RAG 命中结果格式化工具。
 *
 * <p>将检索到的知识片段拼接为可注入 LLM 提示词的上下文字符串。
 */
public final class CustomerServicePrompts {

    private CustomerServicePrompts() {
    }

    /**
     * 将 RAG 命中列表格式化为策略上下文字符串。
     *
     * @param hits 检索命中的知识片段，可为空
     * @return 带引用与分数的多段文本；无命中时返回固定提示
     */
    public static String formatPolicyContext(List<KnowledgeDocument> hits) {
        if (hits == null || hits.isEmpty()) {
            return "未命中知识库。";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            KnowledgeDocument hit = hits.get(i);
            builder.append("片段").append(i + 1)
                .append(" [").append(hit.citation()).append("] score=")
                .append(String.format("%.4f", hit.score()))
                .append("\n")
                .append(hit.content())
                .append("\n\n");
        }
        return builder.toString();
    }
}
