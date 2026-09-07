package com.agentforge.agent.core.spi;

import java.util.Optional;

/**
 * 澄清策略 SPI：缺关键信息时返回追问，不继续调 LLM。
 *
 * <p>可注册多个 Bean，{@link com.agentforge.agent.clarification.ClarificationService}
 * 按 {@link org.springframework.core.annotation.Order} 排序，首个命中的策略生效。
 *
 * <p>平台内置业务主键澄清策略，通过 {@code agent.platform.clarification.biz-key} 配置驱动，
 * 业务无需编写 Java 类即可启用。若需自定义逻辑，可实现本接口并注册为 Spring Bean。
 */
public interface ClarificationPolicy {

    /**
     * 评估当前上下文是否需向用户追问。
     *
     * @param context 澄清判断上下文
     * @return 需澄清时返回结果，否则为空
     */
    Optional<ClarificationResult> evaluate(ClarificationContext context);
}
