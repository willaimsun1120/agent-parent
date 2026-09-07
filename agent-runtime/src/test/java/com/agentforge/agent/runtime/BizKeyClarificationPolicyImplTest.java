package com.agentforge.agent.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentforge.agent.core.config.ClarificationBizKeyProperties;
import com.agentforge.agent.core.spi.ClarificationContext;
import com.agentforge.agent.runtime.BizKeyClarificationPolicyConfiguration.BizKeyClarificationPolicyImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BizKeyClarificationPolicyImplTest {

    private BizKeyClarificationPolicyImpl policy;

    @BeforeEach
    void setUp() {
        ClarificationBizKeyProperties properties = new ClarificationBizKeyProperties();
        properties.setEnabled(true);
        properties.setPattern("ORD-\\d+");
        properties.setTriggerKeywords(List.of("订单", "退款", "支付"));
        properties.setMissingKeyPrompt("请提供订单号");
        properties.setMaxAskTimes(2);
        policy = new BizKeyClarificationPolicyImpl(properties);
    }

    @Test
    void triggersWhenBizKeyMissingAndKeywordMatched() {
        ClarificationContext ctx = new ClarificationContext("这个订单为什么不能退款？", "sess-1", 1, Optional.empty());
        assertThat(policy.evaluate(ctx)).isPresent();
        assertThat(policy.evaluate(ctx).get().message()).isEqualTo("请提供订单号");
    }

    @Test
    void noTriggerWhenBizKeyPresent() {
        ClarificationContext ctx = new ClarificationContext("订单 ORD-1001 为什么不能退款？", "sess-1", 1, Optional.empty());
        assertThat(policy.evaluate(ctx)).isEmpty();
    }

    @Test
    void noTriggerWhenNoKeywordMatched() {
        ClarificationContext ctx = new ClarificationContext("你好", "sess-1", 1, Optional.empty());
        assertThat(policy.evaluate(ctx)).isEmpty();
    }

    @Test
    void noTriggerWhenQuestionBlank() {
        ClarificationContext ctx = new ClarificationContext("  ", "sess-1", 1, Optional.empty());
        assertThat(policy.evaluate(ctx)).isEmpty();
    }

    @Test
    void respectsMaxAskTimes() {
        String sessionId = "sess-max";
        ClarificationContext ctx1 = new ClarificationContext("退款多久到账", sessionId, 1, Optional.empty());
        ClarificationContext ctx2 = new ClarificationContext("退款多久到账", sessionId, 2, Optional.empty());
        ClarificationContext ctx3 = new ClarificationContext("退款多久到账", sessionId, 3, Optional.empty());

        assertThat(policy.evaluate(ctx1)).isPresent();
        assertThat(policy.evaluate(ctx2)).isPresent();
        assertThat(policy.evaluate(ctx3)).isEmpty();
    }

    @Test
    void resetsCounterWhenBizKeyFound() {
        String sessionId = "sess-reset";
        ClarificationContext ctx1 = new ClarificationContext("退款多久到账", sessionId, 1, Optional.empty());
        ClarificationContext ctx2 = new ClarificationContext("退款多久到账", sessionId, 2, Optional.empty());
        ClarificationContext ctxWithKey = new ClarificationContext("ORD-1001 退款多久到账", sessionId, 3, Optional.empty());
        ClarificationContext ctx3 = new ClarificationContext("退款多久到账", sessionId, 4, Optional.empty());

        assertThat(policy.evaluate(ctx1)).isPresent();
        assertThat(policy.evaluate(ctx2)).isPresent();
        assertThat(policy.evaluate(ctxWithKey)).isEmpty();
        assertThat(policy.evaluate(ctx3)).isPresent();
    }

    @Test
    void nullQuestionReturnsEmpty() {
        ClarificationContext ctx = new ClarificationContext(null, "sess-1", 1, Optional.empty());
        assertThat(policy.evaluate(ctx)).isEmpty();
    }
}
