package com.agentforge.agent.core.api;

/**
 * 待人工确认的 HITL 写操作视图，随 {@link AgentChatResponse} 返回给前端展示。
 *
 * @param actionId    动作唯一 ID，confirm 接口使用
 * @param actionType  动作类型，路由到对应 {@link com.agentforge.agent.core.spi.ActionExecutor}
 * @param businessKey 关联业务主键（订单号、工号等，由业务 Demo 定义）
 * @param status      当前状态，通常为 pending
 * @param summary     面向用户的操作摘要
 */
public record AgentPendingActionView(
    String actionId,
    String actionType,
    String businessKey,
    String status,
    String summary
) {
}
