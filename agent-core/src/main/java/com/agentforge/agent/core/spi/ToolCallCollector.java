package com.agentforge.agent.core.spi;

import java.util.List;

/**
 * 框架模式（LangChain4j / AgentScope）工具调用收集器 SPI。
 *
 * <p>由 {@link FrameworkToolRegistrar} 在创建 Tool 时注入，
 * 记录模型实际调用的工具名与入参，最终写入 {@link com.agentforge.agent.core.api.AgentChatResponse#toolCalls}。
 */
public interface ToolCallCollector {

    /**
     * 记录一次工具调用。
     *
     * @param toolName 工具名称
     * @param input    调用入参摘要（通常已序列化）
     */
    void recordCall(String toolName, String input);

    /**
     * @return 本轮已收集的工具调用记录列表
     */
    List<String> toolCalls();
}
