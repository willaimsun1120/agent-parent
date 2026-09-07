package com.agentforge.agent.core.mode;

import com.agentforge.agent.core.api.AgentChatRequest;
import com.agentforge.agent.core.api.AgentChatResponse;

/**
 * Agent 运行模式 SPI（平台内置三种实现，一般无需业务扩展）。
 *
 * <p>实现类由 Spring 收集到 {@link com.agentforge.agent.runtime.AgentModeRegistry}，
 * {@link com.agentforge.agent.runtime.AgentOrchestrator} 按请求的 mode 字段路由。
 *
 * <p>业务扩展应优先实现 {@link com.agentforge.agent.core.spi.ManualAgentExecutor}、
 * {@link com.agentforge.agent.core.spi.FrameworkToolRegistrar} 等，而非新增 Handler。
 */
public interface AgentModeHandler {

    /** @return 本 Handler 对应的模式枚举 */
    AgentMode mode();

    /**
     * 处理一次问答。
     *
     * @param request 客户端请求
     * @return 含 traceId、toolCalls、pendingActions 等的完整响应
     */
    AgentChatResponse chat(AgentChatRequest request);
}
