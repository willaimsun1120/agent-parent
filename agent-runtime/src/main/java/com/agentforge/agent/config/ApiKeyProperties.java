package com.agentforge.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * API Key 鉴权配置项，绑定 {@code app.security.*}。
 *
 * <p>由 {@link ApiKeyAuthFilter} 读取，控制 HTTP 接口是否校验 {@code X-API-Key} 请求头。
 */
@ConfigurationProperties(prefix = "app.security")
public record ApiKeyProperties(
    /** 是否启用 API Key 鉴权 */
    boolean enabled,
    /** 期望的 API Key 值 */
    String apiKey
) {
    /**
     * 是否实际需要鉴权（enabled 且 apiKey 非空）。
     *
     * @return 需要鉴权时为 true
     */
    public boolean requiresAuth() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
