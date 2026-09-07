package com.agentforge.agent.core.mode;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * {@link AgentMode#from(String)} 解析逻辑测试。
 */
class AgentModeTest {

    /** 空值应回退到默认 manual 模式。 */
    @Test
    void defaultsToManualMode() {
        assertThat(AgentMode.from(null)).isEqualTo(AgentMode.MANUAL);
        assertThat(AgentMode.from("")).isEqualTo(AgentMode.MANUAL);
    }

    /** code 与枚举名均应大小写不敏感匹配 agentscope。 */
    @Test
    void resolvesAgentScopeModeIgnoringCase() {
        assertThat(AgentMode.from("AGENTSCOPE")).isEqualTo(AgentMode.AGENTSCOPE);
        assertThat(AgentMode.from("agentscope")).isEqualTo(AgentMode.AGENTSCOPE);
    }

    /** code 与枚举名均应大小写不敏感匹配 langchain4j。 */
    @Test
    void resolvesLangChain4jModeIgnoringCase() {
        assertThat(AgentMode.from("LANGCHAIN4J")).isEqualTo(AgentMode.LANGCHAIN4J);
        assertThat(AgentMode.from("langchain4j")).isEqualTo(AgentMode.LANGCHAIN4J);
    }
}
