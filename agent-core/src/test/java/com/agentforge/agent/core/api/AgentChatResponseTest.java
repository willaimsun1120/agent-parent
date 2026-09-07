package com.agentforge.agent.core.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link AgentChatResponse} 契约测试：验证 trace、工具调用与 runtime 等可观测字段。
 */
class AgentChatResponseTest {

    /** 兼容构造应正确携带 traceId、toolCalls 与 runtime。 */
    @Test
    void carriesTraceAndObservabilityData() {
        AgentChatResponse response = new AgentChatResponse(
            "trace-1",
            "订单 ORD-1001 为什么不能退款？",
            "超过 7 天退款窗口",
            120,
            List.of("getOrderDetail(ORD-1001)"),
            List.of("refund-policy.md score=0.9000"),
            "io.agentscope.harness.agent.HarnessAgent"
        );

        assertThat(response.traceId()).isEqualTo("trace-1");
        assertThat(response.toolCalls()).contains("getOrderDetail(ORD-1001)");
        assertThat(response.runtime()).contains("HarnessAgent");
    }
}
