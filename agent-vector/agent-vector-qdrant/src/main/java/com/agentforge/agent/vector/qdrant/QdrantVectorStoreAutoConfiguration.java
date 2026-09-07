package com.agentforge.agent.vector.qdrant;

import com.agentforge.agent.config.QdrantProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * Qdrant 向量库自动装配（{@code agent.vector-store.type=qdrant}，默认）。
 */
@AutoConfiguration
@ConditionalOnProperty(name = "agent.vector-store.type", havingValue = "qdrant", matchIfMissing = true)
@EnableConfigurationProperties(QdrantProperties.class)
@ComponentScan(basePackageClasses = QdrantVectorStore.class)
public class QdrantVectorStoreAutoConfiguration {
}
