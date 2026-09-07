package com.agentforge.agent.runtime;

import com.agentforge.agent.core.spi.ToolCallCollector;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于内存 List 的工具调用收集器。
 *
 * <p>实现 {@link ToolCallCollector} SPI，Manual 模式执行器在工具调用时写入，
 * 最终汇总为 {@code toolCalls} 返回给客户端与可观测性模块。
 */
public class ListToolCallCollector implements ToolCallCollector {
    private final List<String> toolCalls = new ArrayList<>();

    /**
     * 记录一次工具调用（格式：toolName(input)）。
     *
     * @param toolName 工具名称
     * @param input 入参摘要
     */
    @Override
    public void recordCall(String toolName, String input) {
        toolCalls.add(toolName + "(" + input + ")");
    }

    /**
     * @return 已记录的工具调用列表（只读语义，返回内部 List 引用）
     */
    @Override
    public List<String> toolCalls() {
        return toolCalls;
    }
}
