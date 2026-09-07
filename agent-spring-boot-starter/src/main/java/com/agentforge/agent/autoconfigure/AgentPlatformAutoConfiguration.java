package com.agentforge.agent.autoconfigure;

import com.agentforge.agent.config.ApiKeyProperties;
import com.agentforge.agent.core.config.ClarificationBizKeyProperties;
import com.agentforge.agent.core.config.DashScopeProperties;
import com.agentforge.agent.core.config.AgentPlatformProperties;
import com.agentforge.agent.core.config.RagCategoryProperties;
import com.agentforge.agent.vector.core.VectorStoreProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * Agent 平台自动装配入口（Starter 核心）。
 *
 * <p>业务 Demo 只需引入 {@code agent-spring-boot-starter} 依赖，并实现 SPI（如
 * {@code ManualAgentExecutor}、{@code FrameworkToolRegistrar}），无需手动注册平台 Bean。
 *
 * <p>装配范围：
 * <ul>
 *   <li>{@code ComponentScan}：扫描平台各子包（runtime / rag / hitl / mode 等）</li>
 *   <li>{@code MapperScan}：平台侧 MyBatis Mapper（会话、提示词、可观测、HITL、RAG 评测）</li>
 *   <li>业务 Mapper（订单表、知识库表）由 Demo 启动类的 {@code @MapperScan} 自行扫描</li>
 * </ul>
 */
@AutoConfiguration
@ComponentScan(basePackages = {
    "com.agentforge.agent.action",
    "com.agentforge.agent.api",
    "com.agentforge.agent.cache",
    "com.agentforge.agent.clarification",
    "com.agentforge.agent.config",
    "com.agentforge.agent.conversation",
    "com.agentforge.agent.mode",
    "com.agentforge.agent.observability",
    "com.agentforge.agent.prompt",
    "com.agentforge.agent.rag",
    "com.agentforge.agent.runtime",
    "com.agentforge.agent.session"
})
@MapperScan(basePackages = {
    "com.agentforge.agent.action",
    "com.agentforge.agent.conversation",
    "com.agentforge.agent.observability",
    "com.agentforge.agent.prompt",
    "com.agentforge.agent.rag"
})
@EnableConfigurationProperties({
    AgentPlatformProperties.class,
    ClarificationBizKeyProperties.class,
    RagCategoryProperties.class,
    VectorStoreProperties.class,
    DashScopeProperties.class,
    ApiKeyProperties.class
})
public class AgentPlatformAutoConfiguration {
}
