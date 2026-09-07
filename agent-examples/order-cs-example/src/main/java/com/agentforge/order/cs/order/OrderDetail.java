package com.agentforge.order.cs.order;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单聚合视图（只读 DTO）。
 *
 * <p>由 {@link OrderDetailLoader} 从订单、支付、退款、权益四表聚合而成，
 * 是 Agent 工具 {@link com.agentforge.order.cs.tool.OrderTools} 与 REST API 的统一输出格式。
 * 与平台 SPI 无直接依赖，作为业务事实载体传递给 Agent 层。
 *
 * @param orderNo                 订单号
 * @param userNo                  用户编号
 * @param status                  订单主状态
 * @param amount                  订单金额
 * @param paidAt                  支付完成时间
 * @param completedAt             订单完成时间
 * @param paymentStatus           支付状态（无记录时为 UNKNOWN）
 * @param paymentCallbackStatus   支付回调状态（无记录时为 UNKNOWN）
 * @param refundStatus            退款状态（无记录时为 NONE）
 * @param refundReason            退款原因（无记录时为 null）
 * @param benefits                权益摘要列表，格式为「名称:状态(失败原因)」
 */
public record OrderDetail(
    String orderNo,
    String userNo,
    String status,
    BigDecimal amount,
    LocalDateTime paidAt,
    LocalDateTime completedAt,
    String paymentStatus,
    String paymentCallbackStatus,
    String refundStatus,
    String refundReason,
    List<String> benefits
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
