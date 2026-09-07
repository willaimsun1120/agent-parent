package com.agentforge.agent.core.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 业务主键澄清策略配置，绑定 {@code agent.platform.clarification.biz-key.*}。
 *
 * <p>当用户问题命中触发关键词但未包含业务主键（如订单号、工号）时，
 * 自动返回追问话术，避免 Agent 无上下文盲目回答。
 *
 * <p>支持 {@code max-ask-times} 限制同一会话的最大追问次数，防止无限追问。
 */
@ConfigurationProperties(prefix = "agent.platform.clarification.biz-key")
public class ClarificationBizKeyProperties {

    private boolean enabled = false;
    private String pattern;
    private List<String> triggerKeywords = new ArrayList<>();
    private String missingKeyPrompt;
    private int maxAskTimes = 3;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public List<String> getTriggerKeywords() {
        return triggerKeywords;
    }

    public void setTriggerKeywords(List<String> triggerKeywords) {
        this.triggerKeywords = triggerKeywords;
    }

    public String getMissingKeyPrompt() {
        return missingKeyPrompt;
    }

    public void setMissingKeyPrompt(String missingKeyPrompt) {
        this.missingKeyPrompt = missingKeyPrompt;
    }

    public int getMaxAskTimes() {
        return maxAskTimes;
    }

    public void setMaxAskTimes(int maxAskTimes) {
        this.maxAskTimes = maxAskTimes;
    }
}
