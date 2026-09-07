package com.agentforge.hr.cs;

import com.agentforge.agent.config.ApiKeyProperties;
import com.agentforge.agent.core.config.DashScopeProperties;
import com.agentforge.agent.core.config.AgentPlatformProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * HR 客服 Demo 启动类（业务层入口）。
 */
@SpringBootApplication(scanBasePackages = {
    "com.agentforge.agent",
    "com.agentforge.hr.cs"
})
@MapperScan(basePackages = {
    "com.agentforge.hr.cs.employee",
    "com.agentforge.hr.cs.knowledge"
})
@EnableConfigurationProperties({
    AgentPlatformProperties.class,
    DashScopeProperties.class,
    ApiKeyProperties.class
})
public class HrCsExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(HrCsExampleApplication.class, args);
    }
}
