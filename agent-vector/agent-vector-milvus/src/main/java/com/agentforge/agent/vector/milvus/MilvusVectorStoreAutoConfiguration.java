package com.agentforge.agent.vector.milvus;

import com.agentforge.agent.config.MilvusProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * Milvus 向量库自动装配（{@code agent.vector-store.type=milvus}）。
 */
@AutoConfiguration
@ConditionalOnProperty(name = "agent.vector-store.type", havingValue = "milvus")
@EnableConfigurationProperties(MilvusProperties.class)
@ComponentScan(basePackageClasses = MilvusVectorStore.class)
public class MilvusVectorStoreAutoConfiguration {
}
