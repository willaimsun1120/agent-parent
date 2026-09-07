package com.agentforge.agent.vector.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 向量库选型配置，绑定 {@code agent.vector-store.*}。
 */
@ConfigurationProperties(prefix = "agent.vector-store")
public class VectorStoreProperties {

    /** qdrant（默认）或 milvus */
    private String type = "qdrant";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
