package com.agentforge.agent.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * {@link CustomerServicePlainFormatter} 单元测试。
 *
 * <p>验证引用脚注剥离与事实摘要委托的核心行为。
 */
class CustomerServicePlainFormatterTest {

    /** 应去除回答末尾「依据：…」类引用行。 */
    @Test
    void stripsTrailingCitationFooter() {
        String answer = """
            订单 ORD-1002 的权益还在发放中，请稍等。

            依据：member-benefits v1 · 会员权益说明""";

        assertThat(CustomerServicePlainFormatter.stripCitationFooter(answer))
            .isEqualTo("订单 ORD-1002 的权益还在发放中，请稍等。");
    }

    /** 无 FactSummarizer SPI 时应返回原始文本。 */
    @Test
    void returnsRawContextWhenNoSummarizer() {
        String json = """
            {"orderNo":"ORD-1002","status":"PENDING_PAYMENT_CALLBACK","amount":59.00}
            """;
        String result = CustomerServicePlainFormatter.summarizeFacts(json, java.util.Optional.empty());
        assertThat(result).isEqualTo(json.trim());
    }

    /** 有 FactSummarizer SPI 时应委托其处理。 */
    @Test
    void delegatesToSummarizerWhenPresent() {
        String json = """
            {"orderNo":"ORD-1002","status":"PENDING_PAYMENT_CALLBACK"}
            """;
        com.agentforge.agent.core.spi.FactSummarizer summarizer = factJson -> "摘要：" + factJson.length();
        String result = CustomerServicePlainFormatter.summarizeFacts(json, java.util.Optional.of(summarizer));
        assertThat(result).startsWith("摘要：");
    }
}
