package com.agentforge.agent.core.spi;

/**
 * HITL 写操作执行上下文，由 {@link com.agentforge.agent.action.AgentActionService#confirm} 传入 {@link ActionExecutor}。
 *
 * @param actionId    待确认动作 ID
 * @param orderNo     关联业务订单号
 * @param payloadJson 动作载荷 JSON（创建 pending 时写入）
 * @param sessionId   所属会话 ID
 * @param traceId     链路 ID
 */
public record ActionExecutionContext(
    String actionId,
    String orderNo,
    String payloadJson,
    String sessionId,
    String traceId
) {
}
