package com.agentforge.order.cs.action;

import com.agentforge.agent.core.spi.ActionExecutionContext;
import com.agentforge.agent.core.spi.ActionExecutor;
import com.agentforge.order.cs.cache.CachedOrderDetailService;
import com.agentforge.order.cs.simulation.OrderSimulationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 权益重试申请执行器。
 *
 * <p>实现平台 SPI {@link ActionExecutor}，动作类型 {@code RETRY_BENEFIT}。
 * 人工确认后调用 {@link OrderSimulationService#simulateBenefitIssue} 模拟补发成功，
 * 对应 Agent 工具 {@link com.agentforge.order.cs.tool.OrderTools#retryBenefitIssue} 创建的待确认动作。
 */
@Component
public class RetryBenefitActionExecutor implements ActionExecutor {
    private static final Logger log = LoggerFactory.getLogger(RetryBenefitActionExecutor.class);

    private final OrderSimulationService simulationService;
    private final CachedOrderDetailService cachedOrderDetailService;

    /**
     * 构造权益重试执行器。
     *
     * @param simulationService        订单模拟服务
     * @param cachedOrderDetailService 订单缓存服务
     */
    public RetryBenefitActionExecutor(OrderSimulationService simulationService,
                                        CachedOrderDetailService cachedOrderDetailService) {
        this.simulationService = simulationService;
        this.cachedOrderDetailService = cachedOrderDetailService;
    }

    /**
     * 返回本执行器支持的动作类型。
     *
     * @return {@code RETRY_BENEFIT}
     */
    @Override
    public String actionType() {
        return "RETRY_BENEFIT";
    }

    /**
     * 执行权益重试：模拟权益发放成功并失效订单缓存。
     *
     * @param context 平台传入的动作执行上下文
     */
    @Override
    public void execute(ActionExecutionContext context) {
        simulationService.simulateBenefitIssue(context.orderNo(), "SUCCESS");
        cachedOrderDetailService.evict(context.orderNo());
        log.info("权益重试申请执行完成 actionId={} orderNo={}", context.actionId(), context.orderNo());
    }
}
