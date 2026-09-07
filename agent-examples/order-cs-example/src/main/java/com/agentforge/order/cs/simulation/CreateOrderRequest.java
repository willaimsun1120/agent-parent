package com.agentforge.order.cs.simulation;

import java.math.BigDecimal;

/**
 * 模拟下单请求 DTO。
 *
 * <p>由 {@link SimulationController} 接收，传递给 {@link OrderSimulationService#createOrder}。
 *
 * @param userNo  用户编号
 * @param amount  订单金额
 * @param channel 支付渠道
 */
public record CreateOrderRequest(
    String userNo,
    BigDecimal amount,
    String channel
) {
}
