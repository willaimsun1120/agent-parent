package com.agentforge.agent.core.spi;

/**
 * HITL 写操作执行 SPI（人工 confirm 后的真正落库逻辑）。
 *
 * <p>{@link com.agentforge.agent.action.AgentActionService#confirm} 根据 actionType 路由到对应实现。
 * 创建待确认记录由业务工具调用 {@code AgentActionService.createPending}，不在本接口内。
 *
 * <p>订单 Demo：{@code SubmitRefundActionExecutor}、{@code RetryBenefitActionExecutor}
 */
public interface ActionExecutor {

    /**
     * @return 唯一动作类型，与 createPending 时的 actionType 一致
     */
    String actionType();

    /**
     * 人工确认后执行写操作（改状态、插入退款单等）。
     *
     * @param context 动作执行上下文
     */
    void execute(ActionExecutionContext context);
}
