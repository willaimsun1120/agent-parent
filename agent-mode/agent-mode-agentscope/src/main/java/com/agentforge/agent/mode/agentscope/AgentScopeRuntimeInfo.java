package com.agentforge.agent.mode.agentscope;

import io.agentscope.harness.agent.HarnessAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AgentScope 运行时信息组件。
 *
 * <p>启动时记录已加载的 Harness 类名，供响应 runtime 字段展示。
 */
@Component
public class AgentScopeRuntimeInfo {
    private static final Logger log = LoggerFactory.getLogger(AgentScopeRuntimeInfo.class);

    /** 构造时记录 AgentScope Harness 类加载信息。 */
    public AgentScopeRuntimeInfo() {
        log.info("AgentScope Java 2.x runtime loaded, harnessClass={}", HarnessAgent.class.getName());
    }

    /**
     * 获取 AgentScope Harness 代理类全名。
     *
     * @return Harness 类名
     */
    public String harnessClassName() {
        return HarnessAgent.class.getName();
    }
}
