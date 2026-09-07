package com.agentforge.agent.core.spi;

import java.util.List;
import java.util.Optional;

/**
 * LangChain4j / AgentScope 模式的工具注册 SPI（新业务 Demo 需实现）。
 *
 * <p>将业务操作（查订单、搜知识库、发起 HITL 写操作等）包装为框架可识别的 Tool，
 * 由模型在 ReAct 循环中自主选择调用。
 *
 * <p>订单 Demo 参考：{@code com.agentforge.order.cs.mode.OrderFrameworkToolRegistrar}
 */
public interface FrameworkToolRegistrar {

    /**
     * 创建 LangChain4j 模式可用的工具对象。
     *
     * @param collector 工具调用收集器
     * @param context   会话上下文
     * @return 带 {@code @Tool} 注解的工具 Bean 或工具集合
     */
    Object createLangChain4jTools(ToolCallCollector collector, ToolContext context);

    /**
     * 创建 AgentScope 模式可注册的工具适配器。
     *
     * @param collector 工具调用收集器
     * @param context   会话上下文
     * @return AgentScope 可 register 的工具对象
     */
    Object createAgentScopeTools(ToolCallCollector collector, ToolContext context);

    /**
     * @return 未调工具时写入响应 toolCalls 的提示（展示可用工具名）
     */
    List<String> toolNamesHint();

    /**
     * 判定该问题是否理应触发工具调用。
     *
     * @param question 用户问题
     * @return {@code true} 表示期望有工具调用，用于触发降级或 AgentScope 重试
     */
    boolean expectsToolCalls(String question);

    /**
     * 框架模式工具注册时的会话上下文。
     *
     * @param sessionId 会话 ID
     * @param traceId   链路 ID
     * @param turnIndex 当前轮次
     * @param question  本轮问题
     * @param category  RAG 分类，可选
     */
    record ToolContext(
        String sessionId,
        String traceId,
        int turnIndex,
        String question,
        Optional<String> category
    ) {
    }
}
