package com.agentforge.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Milvus 向量库连接配置，绑定 {@code milvus.*}。
 */
@ConfigurationProperties(prefix = "milvus")
public record MilvusProperties(
    String host,
    int port,
    String collection,
    int vectorSize
) {
    public String uri() {
        return "http://" + host + ":" + port;
    }
}
