package com.agentforge.agent.core.spi;

import com.agentforge.agent.core.api.AgentChatRequest;
import com.agentforge.agent.core.api.AgentChatResponse;
import java.util.List;
import java.util.Optional;

/**
 * manual 模式业务编排 SPI（新业务 Demo 必须实现）。
 *
 * <p>平台 {@link com.agentforge.agent.mode.manual.ManualAgentModeHandler} 在完成澄清后调用本接口。
 * 典型实现：解析实体（订单号）→ 调业务 Service / Tool → RAG 检索 → 拼装 prompt → 调 LLM。
 *
 * <p>订单 Demo 参考：{@code com.agentforge.order.cs.mode.OrderManualAgentExecutor}
 */
public interface ManualAgentExecutor {

    /**
     * 执行一次 manual 模式问答编排。
     *
     * @param context 平台组装的只读上下文
     * @return 回答及工具/RAG 轨迹
     */
    ManualExecutionResult execute(ManualExecutionContext context);

    /**
     * manual 模式执行上下文，由平台在调用前组装。
     *
     * @param request       原始请求
     * @param traceId       链路 ID
     * @param sessionId     会话 ID
     * @param turnIndex     当前轮次
     * @param question      本轮问题（可能经澄清补全）
     * @param history       多轮历史，已格式化
     * @param category      RAG 分类，可选
     * @param recordMetrics 是否记录可观测指标
     */
    record ManualExecutionContext(
        AgentChatRequest request,
        String traceId,
        String sessionId,
        int turnIndex,
        String question,
        String history,
        Optional<String> category,
        boolean recordMetrics
    ) {
    }

    /**
     * manual 模式执行结果。
     *
     * @param response  完整响应体
     * @param toolCalls 工具调用记录（可与 response 中字段冗余，供指标采集）
     * @param ragHits   RAG 命中摘要
     */
    record ManualExecutionResult(
        AgentChatResponse response,
        List<String> toolCalls,
        List<String> ragHits
    ) {
    }
}
