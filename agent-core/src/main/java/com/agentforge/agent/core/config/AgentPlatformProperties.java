package com.agentforge.agent.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Agent 平台全局配置，绑定 {@code agent.platform.*} 前缀。
 *
 * <p>由 agent-runtime 模块读取，控制默认模式、澄清开关、多轮历史上限等；
 * 业务 Demo 在 application.yml 中设置 {@code biz-domain} / {@code biz-module} 标识领域。
 */
@ConfigurationProperties(prefix = "agent.platform")
public class AgentPlatformProperties {

    /** 业务领域，由 Demo 模块在 application.yml 中配置，如 order / hr */
    private String bizDomain = "";
    /** 业务模块，由 Demo 模块在 application.yml 中配置，如 cs */
    private String bizModule = "";
    private Conversation conversation = new Conversation();
    private Clarification clarification = new Clarification();
    private Modes modes = new Modes();

    public String getBizDomain() {
        return bizDomain;
    }

    public void setBizDomain(String bizDomain) {
        this.bizDomain = bizDomain;
    }

    public String getBizModule() {
        return bizModule;
    }

    public void setBizModule(String bizModule) {
        this.bizModule = bizModule;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public Clarification getClarification() {
        return clarification;
    }

    public void setClarification(Clarification clarification) {
        this.clarification = clarification;
    }

    public Modes getModes() {
        return modes;
    }

    public void setModes(Modes modes) {
        this.modes = modes;
    }

    /** 多轮会话相关配置。 */
    public static class Conversation {
        /** 拼入 prompt 的最大历史轮数 */
        private int maxHistoryTurns = 6;

        public int getMaxHistoryTurns() {
            return maxHistoryTurns;
        }

        public void setMaxHistoryTurns(int maxHistoryTurns) {
            this.maxHistoryTurns = maxHistoryTurns;
        }
    }

    /** 澄清策略全局开关。 */
    public static class Clarification {
        /** 是否启用澄清策略链 */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /** 运行模式相关配置。 */
    public static class Modes {
        /** 请求未指定 mode 时的默认值，对应 {@link com.agentforge.agent.core.mode.AgentMode#code} */
        private String defaultMode = "manual";

        public String getDefaultMode() {
            return defaultMode;
        }

        public void setDefaultMode(String defaultMode) {
            this.defaultMode = defaultMode;
        }
    }
}
