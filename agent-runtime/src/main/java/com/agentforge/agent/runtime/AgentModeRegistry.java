package com.agentforge.agent.runtime;

import com.agentforge.agent.core.mode.AgentMode;
import com.agentforge.agent.core.mode.AgentModeHandler;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Agent 模式注册表。
 *
 * <p>Spring 启动时注入所有 {@link AgentModeHandler} Bean，按 {@link AgentMode} 建立索引。
 * {@link AgentOrchestrator} 通过 {@link #get} 获取 Handler，业务侧一般无需修改本类。
 *
 * <p>内置 Handler：ManualAgentModeHandler、LangChain4jAgentModeHandler、AgentScopeAgentModeHandler
 * （分布在 agent-mode-* 模块，由 Starter 扫描注册）。
 */
@Component
public class AgentModeRegistry {

    private final Map<AgentMode, AgentModeHandler> handlers;

    /**
     * @param handlerList Spring 注入的全部 AgentModeHandler
     */
    public AgentModeRegistry(List<AgentModeHandler> handlerList) {
        this.handlers = new EnumMap<>(AgentMode.class);
        for (AgentModeHandler handler : handlerList) {
            AgentModeHandler existing = handlers.putIfAbsent(handler.mode(), handler);
            if (existing != null) {
                throw new IllegalStateException("重复的 Agent 模式处理器: " + handler.mode());
            }
        }
    }

    /**
     * 按模式获取 Handler，未注册则抛异常。
     *
     * @param mode Agent 运行模式
     * @return 对应的模式处理器
     */
    public AgentModeHandler get(AgentMode mode) {
        AgentModeHandler handler = handlers.get(mode);
        if (handler == null) {
            throw new IllegalStateException("未注册的 Agent 模式: " + mode);
        }
        return handler;
    }
}
