package com.agentforge.agent.runtime;

import com.agentforge.agent.core.spi.FactSummarizer;
import java.util.Optional;

/**
 * 客服场景下的回答清洗工具。
 *
 * <p>位于 agent-runtime 层，去除 LLM 回答末尾的引用脚注，
 * 并委托业务 {@link FactSummarizer} SPI 实现（如果存在）将 JSON 事实转为自然语言摘要。
 */
public final class CustomerServicePlainFormatter {

    private CustomerServicePlainFormatter() {
    }

    /**
     * 去掉回答末尾 LLM 可能附带的「依据：doc · 标题」等引用行。
     *
     * @param answer 原始回答
     * @return 去除引用脚注后的回答
     */
    public static String stripCitationFooter(String answer) {
        if (answer == null || answer.isBlank()) {
            return answer == null ? "" : answer;
        }
        String trimmed = answer.stripTrailing();
        while (true) {
            String next = trimmed.replaceFirst("(?s)(\\R\\s*依据[：:].*)$", "")
                .replaceFirst("(?s)(\\R\\s*参考[：:].*)$", "")
                .replaceFirst("(?s)(\\R\\s*引用[：:].*)$", "")
                .stripTrailing();
            if (next.equals(trimmed)) {
                return trimmed;
            }
            trimmed = next;
        }
    }

    /**
     * 将工具上下文中的 JSON 片段汇总为客服可读的中文摘要。
     *
     * <p>优先委托业务 {@link FactSummarizer} SPI 实现；若未注册则返回原始文本。
     *
     * @param rawContext 可能含多个 JSON 对象的原始文本
     * @param summarizer 业务事实摘要 SPI（可选）
     * @return 多行中文摘要，无 SPI 时返回原始文本
     */
    public static String summarizeFacts(String rawContext, Optional<FactSummarizer> summarizer) {
        if (rawContext == null || rawContext.isBlank()) {
            return "暂无数据。";
        }
        if (summarizer.isPresent()) {
            return summarizer.get().summarize(rawContext);
        }
        return rawContext.stripTrailing();
    }
}
