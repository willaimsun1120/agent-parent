package com.agentforge.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Qdrant 向量数据库连接配置，绑定 {@code qdrant.*} 属性。
 */
@ConfigurationProperties(prefix = "qdrant")
public record QdrantProperties(
    String host,
    int port,
    String collection,
    int vectorSize
) {
    /** 拼接 Qdrant HTTP 基础地址。 */
    public String baseUrl() {
        return "http://" + host + ":" + port;
    }
}
