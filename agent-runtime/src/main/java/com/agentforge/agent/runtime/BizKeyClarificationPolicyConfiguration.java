package com.agentforge.agent.runtime;

import com.agentforge.agent.core.config.ClarificationBizKeyProperties;
import com.agentforge.agent.core.spi.ClarificationContext;
import com.agentforge.agent.core.spi.ClarificationPolicy;
import com.agentforge.agent.core.spi.ClarificationResult;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * 业务主键澄清策略自动装配。
 *
 * <p>当 {@code agent.platform.clarification.biz-key.enabled=true} 时注册通用澄清策略，
 * 业务只需在 YAML 中配置主键正则、触发关键词和追问话术，无需编写 Java 类。
 *
 * <p>若业务自定义了 {@link ClarificationPolicy} Bean，则本配置不生效。
 */
@Configuration
public class BizKeyClarificationPolicyConfiguration {

    private static final Logger log = LoggerFactory.getLogger(BizKeyClarificationPolicyConfiguration.class);

    @Bean
    @Order(0)
    @ConditionalOnProperty(prefix = "agent.platform.clarification.biz-key", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(ClarificationPolicy.class)
    ClarificationPolicy bizKeyClarificationPolicy(ClarificationBizKeyProperties properties) {
        log.info("业务主键澄清策略已装配 pattern={} triggerKeywords={} maxAskTimes={}",
            properties.getPattern(), properties.getTriggerKeywords(), properties.getMaxAskTimes());
        return new BizKeyClarificationPolicyImpl(properties);
    }

    static class BizKeyClarificationPolicyImpl implements ClarificationPolicy {

        private final ClarificationBizKeyProperties properties;
        private final Pattern compiledPattern;
        private final ConcurrentHashMap<String, int[]> sessionAskCounts = new ConcurrentHashMap<>();

        BizKeyClarificationPolicyImpl(ClarificationBizKeyProperties properties) {
            this.properties = properties;
            this.compiledPattern = Pattern.compile(properties.getPattern());
        }

        @Override
        public Optional<ClarificationResult> evaluate(ClarificationContext context) {
            String question = context.question();
            if (question == null || question.isBlank()) {
                return Optional.empty();
            }

            String sessionId = context.sessionId();

            if (compiledPattern.matcher(question).find()) {
                sessionAskCounts.remove(sessionId);
                return Optional.empty();
            }

            boolean needsBizKey = properties.getTriggerKeywords().stream()
                .anyMatch(question::contains);
            if (!needsBizKey) {
                return Optional.empty();
            }

            int askCount = sessionAskCounts.computeIfAbsent(sessionId, k -> new int[1])[0]++;
            if (askCount >= properties.getMaxAskTimes()) {
                log.debug("澄清已达上限 askCount={} maxAskTimes={} sessionId={}",
                    askCount, properties.getMaxAskTimes(), sessionId);
                return Optional.empty();
            }

            log.info("业务主键澄清命中 askCount={}/{} sessionId={}",
                askCount + 1, properties.getMaxAskTimes(), sessionId);
            return Optional.of(new ClarificationResult(
                properties.getMissingKeyPrompt().trim(),
                "missing_biz_key"));
        }
    }
}
