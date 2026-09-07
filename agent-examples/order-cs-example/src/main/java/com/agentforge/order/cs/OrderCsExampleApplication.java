package com.agentforge.order.cs;

import com.agentforge.agent.config.ApiKeyProperties;
import com.agentforge.agent.core.config.DashScopeProperties;
import com.agentforge.agent.core.config.AgentPlatformProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 订单客服 Demo 启动类（业务层入口）。
 *
 * <p>接入步骤参考：
 * <ol>
 *   <li>引入 agent-spring-boot-starter（自动装配平台）</li>
 *   <li>{@code scanBasePackages} 扫描平台包 + 业务包</li>
 *   <li>{@code @MapperScan} 扫描业务 Mapper（平台 Mapper 由 Starter 扫描）</li>
 *   <li>实现 SPI 并注册为 Spring Bean：ManualAgentExecutor、FrameworkToolRegistrar、ClarificationPolicy、ActionExecutor 等</li>
 * </ol>
 */
@SpringBootApplication(scanBasePackages = {
    "com.agentforge.agent",      // 平台 Bean（由 Starter 引入后仍需扫描或依赖 AutoConfiguration）
    "com.agentforge.order.cs"    // 本 Demo 业务 Bean
})
@MapperScan(basePackages = {
    "com.agentforge.order.cs.order",      // 订单 / 支付 / 退款 / 权益
    "com.agentforge.order.cs.knowledge"  // 知识库文章与切片
})
@EnableConfigurationProperties({
    AgentPlatformProperties.class,
    DashScopeProperties.class,
    ApiKeyProperties.class
})
public class OrderCsExampleApplication {

    /**
     * 启动 Spring Boot 应用，加载平台 Starter 与本 Demo 业务 Bean。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderCsExampleApplication.class, args);
    }
}
