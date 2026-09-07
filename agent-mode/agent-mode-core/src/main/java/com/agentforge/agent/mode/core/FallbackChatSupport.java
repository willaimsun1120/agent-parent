package com.agentforge.agent.mode.core;

import com.agentforge.agent.core.api.AgentChatRequest;
import com.agentforge.agent.core.api.AgentChatResponse;

/**
 * 手写模式降级接口。
 *
 * <p>从 {@code ManualAgentModeHandler} 提取，供 {@link FrameworkModeSupport} 和
 * {@link AgentFallbackSupport} 在编译期解耦 agent-mode-manual 模块。
 */
public interface FallbackChatSupport {

    /**
     * 供框架模式降级调用，避免重复计数 metrics。
     *
     * @param request 对话请求
     * @return 对话响应
     */
    AgentChatResponse chatAsFallback(AgentChatRequest request);
}
