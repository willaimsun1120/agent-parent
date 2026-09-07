package com.agentforge.agent.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 阿里云百炼（DashScope）API 配置，绑定 {@code dashscope.*} 属性。
 *
 * <p>位于 agent-core，供 agent-rag、agent-mode-langchain4j、agent-mode-agentscope 等多模块共享，
 * 避免框架模式对 rag 模块产生不必要的依赖。
 *
 * @param apiKey         API 密钥
 * @param chatModel      对话模型名称
 * @param embeddingModel 向量化模型名称
 */
@ConfigurationProperties(prefix = "dashscope")
public record DashScopeProperties(
    String apiKey,
    String chatModel,
    String embeddingModel
) {
    /**
     * 判断是否已配置有效的 API Key。
     *
     * @return 非空且非空白时返回 {@code true}
     */
    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
