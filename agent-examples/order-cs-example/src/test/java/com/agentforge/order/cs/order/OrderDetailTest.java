package com.agentforge.order.cs.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link OrderDetail} 记录类型单元测试。
 *
 * <p>验证聚合 DTO 正确暴露订单号、支付状态、退款状态等 Agent 提示词所需字段。
 */
class OrderDetailTest {
    @Test
    void exposesOrderFactsForAgentPrompt() {
        OrderDetail detail = new OrderDetail(
            "ORD-1001",
            "U1001",
            "COMPLETED",
            BigDecimal.valueOf(199),
            null,
            null,
            "SUCCESS",
            "SUCCESS",
            "REJECTED",
            "Order completed more than 7 days ago.",
            List.of("SVIP monthly bonus:ISSUED")
        );

        assertThat(detail.orderNo()).isEqualTo("ORD-1001");
        assertThat(detail.paymentStatus()).isEqualTo("SUCCESS");
        assertThat(detail.refundStatus()).isEqualTo("REJECTED");
    }
}
