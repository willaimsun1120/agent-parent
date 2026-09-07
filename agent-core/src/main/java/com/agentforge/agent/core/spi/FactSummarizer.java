package com.agentforge.agent.core.spi;

/**
 * 业务事实摘要 SPI：将工具返回的 JSON 事实转为面向用户的自然语言摘要。
 *
 * <p>平台 {@link com.agentforge.agent.runtime.CustomerServicePlainFormatter} 不再硬编码订单格式，
 * 而是委托业务 Demo 提供的本 SPI 实现。
 */
public interface FactSummarizer {

    /**
     * 将 JSON 格式的业务事实转为用户可读的摘要文本。
     *
     * @param factJson 工具返回的 JSON 字符串
     * @return 面向用户的自然语言摘要
     */
    String summarize(String factJson);
}
