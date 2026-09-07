package com.agentforge.agent.clarification;

import com.agentforge.agent.core.config.AgentPlatformProperties;
import com.agentforge.agent.core.spi.ClarificationContext;
import com.agentforge.agent.core.spi.ClarificationPolicy;
import com.agentforge.agent.core.spi.ClarificationResult;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;

/**
 * 澄清链服务（平台层，三种 mode 共用）。
 *
 * <p>在真正调工具 / RAG / LLM 之前，依次执行各 {@link ClarificationPolicy} SPI。
 * 任一策略返回 {@link ClarificationResult} 即短路，直接向用户返回追问话术。
 *
 * <p>示例：平台内置 biz-key 策略 — 问题涉及退款但无 ORD-xxxx 时追问订单号。
 * 可通过 {@code agent.platform.clarification.enabled=false} 关闭。
 */
@Service
public class ClarificationService {
    private static final Logger log = LoggerFactory.getLogger(ClarificationService.class);

    private final AgentPlatformProperties properties;
    private final List<ClarificationPolicy> policies;

    /**
     * @param properties 平台配置（含澄清开关）
     * @param policies 按 @Order 排序的澄清策略 SPI 列表
     */
    public ClarificationService(AgentPlatformProperties properties, List<ClarificationPolicy> policies) {
        this.properties = properties;
        this.policies = policies.stream()
            .sorted(AnnotationAwareOrderComparator.INSTANCE)
            .toList();
        log.info("ClarificationService 初始化 policies={} enabled={}", this.policies.size(), properties.getClarification().isEnabled());
    }

    /**
     * 依次执行澄清策略链，任一命中则短路返回追问结果。
     *
     * @param context 澄清上下文（sessionId、问题、轮次等）
     * @return 命中时含追问话术的 ClarificationResult，未命中为空
     */
    public Optional<ClarificationResult> evaluate(ClarificationContext context) {
        if (!properties.getClarification().isEnabled()) {
            log.debug("澄清已禁用 sessionId={} turnIndex={}", context.sessionId(), context.turnIndex());
            return Optional.empty();
        }
        log.info("开始澄清链评估 sessionId={} turnIndex={} questionLength={}",
            context.sessionId(), context.turnIndex(), context.question() == null ? 0 : context.question().length());
        for (ClarificationPolicy policy : policies) {
            Optional<ClarificationResult> result = policy.evaluate(context);
            if (result.isPresent()) {
                log.info("澄清命中 policy={} reason={} sessionId={}",
                    policy.getClass().getSimpleName(), result.get().reason(), context.sessionId());
                return result;
            }
            log.debug("澄清未命中 policy={} sessionId={}", policy.getClass().getSimpleName(), context.sessionId());
        }
        log.info("澄清链未命中 sessionId={} turnIndex={}", context.sessionId(), context.turnIndex());
        return Optional.empty();
    }
}
